package org.amine.security.polling.onlinepollingsystem.system.polling.service

import org.amine.security.polling.onlinepollingsystem.exceptions.RemoveParticipateRightException
import org.amine.security.polling.onlinepollingsystem.exceptions.ValidationDataException
import org.amine.security.polling.onlinepollingsystem.models.permission.Permission
import org.amine.security.polling.onlinepollingsystem.models.polling.ResultPollingStates
import org.amine.security.polling.onlinepollingsystem.models.polling.Status
import org.amine.security.polling.onlinepollingsystem.repos.permissions.PermissionRepository
import org.amine.security.polling.onlinepollingsystem.repos.polling.DuringPollingResultRepository
import org.amine.security.polling.onlinepollingsystem.repos.polling.PollRepository
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.services.polling.PollingResultService
import org.amine.security.polling.onlinepollingsystem.system.polling.model.TrackPoll
import org.amine.security.polling.onlinepollingsystem.system.polling.repo.TrackPollRepository
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.Optional
import java.util.logging.Logger

@Service
class TrackingPollService(
    private val userRepository: UserRepository,
    private val trackPollRepository: TrackPollRepository,
    private val appTool: AppTool,
    private val pollRepository: PollRepository,
    private val permissionRepository: PermissionRepository,
    private val duringPollingResultRepository: DuringPollingResultRepository,
) {
    val logger = Logger.getLogger(PollingResultService::class.java.name)

    fun isAlreadyParticipateInPoll(userId: Long, timeOfPolling: ZonedDateTime?) {
        val trackPollOptional = trackPollRepository.findByUserId(userId)
        var trackPoll = TrackPoll()
        if (trackPollOptional.isPresent) {
            trackPoll = trackPollOptional.get()
            if (trackPoll.endTimeOfRemoveParticipateRight != null) {
                throw RemoveParticipateRightException(
                    "You can't participate in the polling.You have tried three times to participate in a poll. ab ${trackPoll.endTimeOfRemoveParticipateRight} can you participate "
                )
            }
            if (trackPoll.numberOfTry == 3) {
                val user = userRepository.findById(userId).get()
                val permission: Permission = permissionRepository.findByPermission("PARTICIPATE_POLE").get()
                val permissions = user.permissions
                for (p in permissions) {
                    if (p.id == permission.id) {
                        permissions.remove(p)
                        break
                    }
                }
                permission.users.remove(user)
                userRepository.save(user)
                permissionRepository.save(permission)
                trackPoll.startTimeOfRemoveParticipateRight = appTool.getNowTime()
                trackPoll.endTimeOfRemoveParticipateRight =
                    trackPoll.startTimeOfRemoveParticipateRight!!.plusMinutes(10)
                trackPollRepository.save(trackPoll)
                throw RemoveParticipateRightException(
                    "You have tried three times to participate in the poll" +
                            " although you have been given already given your vote. From now you cant vote for the next 24 Hours"
                )
            }
            trackPoll.numberOfTry += 1
        } else {
            trackPoll.userId = userId
            trackPoll.numberOfTry = 1
            trackPoll.startTimeOfRemoveParticipateRight = appTool.getNowTime()
        }
        trackPollRepository.save(trackPoll)
        throw ValidationDataException("You have already participated in this poll in $timeOfPolling")
    }

    /**
     *   separate scheduled one to convert the status of poll to finish
     *   the other to calculate result of all polls. That takes more time the first one
     *   for that it will be scanned all polls to check the finished ones (less time + needed for Frontend):
     *   and the other scheduled makes the calculations
     */
    @Scheduled(cron = "0 */2 * * * *", zone = "Europe/Berlin")
    @Transactional
    fun checkTimeOfPolls() {
        val polls = pollRepository.findAll().filter { !it.deleted }
        logger.info("START changing the status of poll to FINISH when EndPolling is ended.  TrackingTime => ${appTool.getNowTime()} ")
        val endedPolls =
            polls.filter { appTool.getNowTime().isAfter(it.endPollingTime) && it.status == Status.OPEN_ACTIVE.name }
        endedPolls.forEach { poll ->
            poll.status = Status.FINISHED.name
            poll.resultState = ResultPollingStates.PENDING_RESULT.name
            logger.info("Poll with id: ${poll.id} has changed to ${Status.FINISHED.name} and ${ResultPollingStates.PENDING_RESULT.name}. \n")
        }
        logger.info("END changing the status of poll to FINISH when EndPolling is ended.  TrackingTime => ${appTool.getNowTime()} ")
        logger.info("START changing the status of poll to OPEN_ACTIVE when STATUS is PENDING.  TrackingTime => ${appTool.getNowTime()} ")
        val pendingPolls =
            polls.filter { it.startPollingTime!!.isAfter(appTool.getNowTime()) && it.status == Status.PENDING.name }
        pendingPolls.forEach { poll ->
            poll.status = Status.OPEN_ACTIVE.name
            logger.info("Poll with id: ${poll.id} has changed to ${Status.OPEN_ACTIVE.name}. \n")
        }
        logger.info("END changing the status of poll to OPEN_ACTIVE when STATUS is PENDING.  TrackingTime => ${appTool.getNowTime()} ")
    }

    @Scheduled(cron = "0 */4 * * * *", zone = "Europe/Berlin")
    @Transactional
    fun cleanTrackPoll() {
        logger.info("Start removing users form trackPoll table: TrackingTime => ${appTool.getNowTime()} ")
        val trackPolls = trackPollRepository.findAll()
        for (trackPoll in trackPolls) {
            if ((trackPoll.endTimeOfRemoveParticipateRight != null && trackPoll.endTimeOfRemoveParticipateRight!!.isBefore(
                    appTool.getNowTime()
                ) || (trackPoll.startTimeOfRemoveParticipateRight!!.plusMinutes(10).isBefore(appTool.getNowTime())))
            ) {
                trackPollRepository.deleteById(trackPoll.id)
                val optionalPermission: Optional<Permission> = permissionRepository.findByPermission("PARTICIPATE_POLE")
                val optionalUser = userRepository.findById(trackPoll.userId)
                val permission = optionalPermission.get()
                if (optionalUser.isPresent) {
                    val user = optionalUser.get()
                    user.permissions.add(permission)
                    permission.users.add(user)
                    permissionRepository.save(permission)
                    userRepository.save(user)
                    logger.info("User: ${user.username} can again participate in polling. ")
                }
            }
        }
        logger.info("End removing users form trackPoll table. TrackingTime => ${appTool.getNowTime()}  ")
    }

}