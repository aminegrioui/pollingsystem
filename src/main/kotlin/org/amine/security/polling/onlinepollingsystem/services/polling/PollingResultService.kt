package org.amine.security.polling.onlinepollingsystem.services.polling

import org.amine.security.polling.onlinepollingsystem.exceptions.ValidationDataException
import org.amine.security.polling.onlinepollingsystem.models.polling.Poll
import org.amine.security.polling.onlinepollingsystem.models.polling.PollingResult
import org.amine.security.polling.onlinepollingsystem.models.polling.ResultPollingStates
import org.amine.security.polling.onlinepollingsystem.models.polling.Status
import org.amine.security.polling.onlinepollingsystem.repos.polling.PollRepository
import org.amine.security.polling.onlinepollingsystem.repos.polling.DuringPollingResultRepository
import org.amine.security.polling.onlinepollingsystem.repos.polling.PollingResultRepository
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.logging.Logger

@Service
class PollingResultService(
    private val duringPollingResultRepository: DuringPollingResultRepository,
    private val pollRepository: PollRepository,
    private val pollingResultRepository: PollingResultRepository,
    private val appTool: AppTool
) {
    val logger = Logger.getLogger(PollingResultService::class.java.name)

    @Scheduled(cron = "0 */10 * * * *", zone = "Europe/Berlin")
    @Transactional
    fun calculateThePollingResult() {
        logger.info("START calculating the polling after end of polling ")

        val polls: List<Poll> =
            pollRepository.findAll()
                .filter { it.resultState == ResultPollingStates.PENDING_RESULT.name }
                .toList()
        for (poll in polls) {
            val duringPollingResults = duringPollingResultRepository.findAll()
                .filter {
                    it.pollId == poll.id && it.timeOfPolling != null && (it.timeOfPolling!!.isBefore(poll.endPollingTime) || it.timeOfPolling!! == poll.endPollingTime)
                }
            var resultOfPolling = ""
            val pollingResult = PollingResult()
            for (option in poll.options) {
                var optionCounter = 0
                for (resultPolling in duringPollingResults) {
                    val selectedOptions = resultPolling.selectedOptions.split(", ").toSet()
                    if (selectedOptions.contains(option)) {
                        optionCounter += 1;
                    }
                }
                resultOfPolling += "${option}: (${optionCounter}),  "
            }
            pollingResult.pollId = poll.id
            pollingResult.title = poll.title
            pollingResult.description = poll.description
            pollingResult.result = resultOfPolling
            pollingResult.vottingpercentage = ((duringPollingResults.size.toDouble() / poll.participants.size) * 100.0)
            pollingResult.timeOfResult = appTool.getNowTime()
            pollingResultRepository.save(pollingResult)
            poll.resultState = ResultPollingStates.COMPLETED.name
            logger.info("Poll with id ${poll.id} has calculated and stored in DB. \n")
            // delete duringPollingResults of this poll
            duringPollingResultRepository.deleteAll(duringPollingResults)
        }
        logger.info("End calculating the polling after end of polling. \n ")
    }

    fun getResultOfPoll(pollId: Long, userId: Long): PollingResult {
        var pollResult = PollingResult()
        val optionalPoll = pollRepository.findById(pollId)
        if (optionalPoll.isEmpty) {
            throw ValidationDataException("This poll with $pollId is not found ")
        }
        val poll = optionalPoll.get()
        if (poll.deleted) {
            throw ValidationDataException("This poll with $pollId is deleted ")
        }
        if (poll.status == Status.PENDING.name) {
            pollResult.description = "This Poll with id: $pollId has no result cause is am pending"
            return pollResult
        }
        if (poll.status == Status.CANCELED.name) {
            pollResult.description = "This Poll with id: $pollId has no result cause is canceled"
            return pollResult
        }
        if (poll.status == Status.OPEN_ACTIVE.name) {
            pollResult.description = "This Poll with id: $pollId has no result cause is still opened"
            return pollResult
        }
        val optionalPollResult = pollingResultRepository.findByPollId(pollId)
        if (optionalPollResult.isEmpty) {
            pollResult.description =
                "The result of this poll: $pollId is am calculation. Please try again after many minutes"
            return pollResult
        }
        pollResult = optionalPollResult.get()
        if (poll.isPublicResults == false) {
            val participants = poll.participants
            var isNotParticipant = true
            for (participant in participants) {
                if (participant.id == userId) {
                    isNotParticipant = false
                }
            }
            if (isNotParticipant) {
                throw ValidationDataException("This poll: $pollId is a private poll and its result is not public for all. It is just for its Participants")
            }
        }

        return pollResult
    }

}