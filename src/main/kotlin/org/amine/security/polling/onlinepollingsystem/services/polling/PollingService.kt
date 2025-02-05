package org.amine.security.polling.onlinepollingsystem.services.polling

import com.google.common.base.Strings
import org.amine.security.polling.onlinepollingsystem.dtos.poll.request.*
import org.amine.security.polling.onlinepollingsystem.dtos.poll.response.ParticipatePollResponse
import org.amine.security.polling.onlinepollingsystem.dtos.poll.response.FullPollResponseDto
import org.amine.security.polling.onlinepollingsystem.dtos.poll.response.SmallPollResponseDto
import org.amine.security.polling.onlinepollingsystem.enumuration.polling.PollType
import org.amine.security.polling.onlinepollingsystem.enumuration.polling.VisibilityType
import org.amine.security.polling.onlinepollingsystem.exceptions.ValidationDataException
import org.amine.security.polling.onlinepollingsystem.models.polling.*
import org.amine.security.polling.onlinepollingsystem.models.users.User
import org.amine.security.polling.onlinepollingsystem.repos.polling.PollRepository
import org.amine.security.polling.onlinepollingsystem.repos.polling.DuringPollingResultRepository
import org.amine.security.polling.onlinepollingsystem.repos.user.UserRepository
import org.amine.security.polling.onlinepollingsystem.services.user.UserService
import org.amine.security.polling.onlinepollingsystem.system.audit.admin.service.AdminAuditService
import org.amine.security.polling.onlinepollingsystem.system.audit.user.service.UserAuditService
import org.amine.security.polling.onlinepollingsystem.system.blacklist.user.services.BlackListService
import org.amine.security.polling.onlinepollingsystem.system.polling.service.TrackingPollService
import org.amine.security.polling.onlinepollingsystem.system.security.jwt.JwtTool
import org.amine.security.polling.onlinepollingsystem.system.security.jwt.ParsTokenResponse
import org.amine.security.polling.onlinepollingsystem.system.tools.AppTool
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

@Service
class PollingService(
    val jwtTool: JwtTool,
    val blackListService: BlackListService,
    val pollRepository: PollRepository,
    private val userRepository: UserRepository,
    private val categoryService: CategoryService,
    private val appTool: AppTool,
    private val userService: UserService,
    private val duringPollingResultRepository: DuringPollingResultRepository,
    private val pollingResultService: PollingResultService,
    private val trackingPollService: TrackingPollService,
    private val userAuditService: UserAuditService,
    private val adminAuditService: AdminAuditService
) {

    fun createNewPoll(pollCreateDto: PollDto, requestHeader: HttpHeaders): FullPollResponseDto {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        var userOptional: Optional<User>
        val poll = Poll()
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        if (Strings.isNullOrEmpty(pollCreateDto.title)) {
            throw ValidationDataException("A poll must have a title ")
        }
        if (Strings.isNullOrEmpty(pollCreateDto.description)) {
            throw ValidationDataException("A poll must have a description ")
        }
        if (!Strings.isNullOrEmpty(pollCreateDto.pollType)) {
            val setOfTypes =
                setOf(
                    PollType.SINGLE_CHOICE.name.uppercase(Locale.getDefault()),
                    PollType.MULTIPLE_CHOICE.name.uppercase(Locale.getDefault())
                )
            if (!setOfTypes.contains(pollCreateDto.pollType!!.uppercase(Locale.getDefault()))) {
                throw ValidationDataException("Please select one of the Poll Types: $setOfTypes ")
            }
            poll.type = pollCreateDto.pollType!!
        }
        if (Strings.isNullOrEmpty(pollCreateDto.category)) {
            throw ValidationDataException("The poll must have a category")
        }
        val optionalCategory = categoryService.findCategory(pollCreateDto.category)

        if (optionalCategory.isEmpty) {
            throw ValidationDataException("This category ${pollCreateDto.category} is no category: The categories are ${categoryService.getAllCategoriesName()}")
        }
        pollCreateDto.options = pollCreateDto.options.map { it.trim().toLowerCase() }.sorted().toSet()
        if (pollCreateDto.options.isEmpty() || pollCreateDto.options.size == 1) {
            throw ValidationDataException("A poll must have options and at list two options")
        }
        if (pollCreateDto.participated != null && pollCreateDto.participated!!.size < 5) {
            throw ValidationDataException("A poll with private visibility must have  at list five participated")
        }
        if (Strings.isNullOrEmpty(pollCreateDto.endPollingTime)) {
            throw ValidationDataException("A poll must have an End Datetime ")
        }
        if (!appTool.validateDate(pollCreateDto.endPollingTime)) {
            throw ValidationDataException(" EndTime of the  poll must have such format YYYY-MM-DD HH:MM:SS")
        }
        val startTime: ZonedDateTime
        if (!Strings.isNullOrEmpty(pollCreateDto.startPollingTime)) {
            if (!appTool.validateDate(pollCreateDto.startPollingTime)) {
                throw ValidationDataException(" StartTime of the  poll must have such format YYYY-MM-DD HH:MM:SS")
            }
            startTime = appTool.toRightDateFormat(pollCreateDto.startPollingTime)
            if (startTime.isBefore(appTool.getNowTime())) {
                throw ValidationDataException(" StartTime of the  poll must be in the present or future")
            }
        } else {
            startTime = appTool.getNowTime()
        }

        val endTime: ZonedDateTime = appTool.toRightDateFormat(pollCreateDto.endPollingTime)
        if (endTime.isBefore(startTime)) {
            throw ValidationDataException(" EndDateTime of a poll must be after startTime of Polling ")
        }
        if (Duration.between(startTime, endTime).toHours() < 1) {
            throw ValidationDataException("A poll must have a period at least one hours")
        }

        userOptional = userRepository.findById(parseToken.id)
        val user: User = userOptional.get()
        poll.creator = user
        poll.title = pollCreateDto.title
        poll.description = pollCreateDto.description
        if (user.polls.contains(poll)) {
            throw ValidationDataException("This poll with the title: ${pollCreateDto.title} and description: ${pollCreateDto.description} is already exist")
        }

        var participated: MutableSet<User> = mutableSetOf()
        val noUsers: MutableSet<Long> = mutableSetOf()
        if (pollCreateDto.participated != null) {
            for (item: Long in pollCreateDto.participated!!) {
                userOptional = userRepository.findById(item)
                if (userOptional.isPresent) {
                    val participate = userOptional.get()
                    if (!participate.isDeleted) {
                        participated.add(participate)
                    }

                } else {
                    noUsers.add(item)
                }
            }
            participated.add(user)
            if (participated.size < 5) {
                throw ValidationDataException("A poll with private visibility must have  at list five participated, Some of the given users are not found or deleted: $noUsers")
            }
            poll.visibility = VisibilityType.PRIVATE.name
            poll.participants.addAll(participated)
        } else {
            participated = userRepository.findAll().filter { !it.isDeleted }.toHashSet()
        }

        if (pollCreateDto.isPublicResults != null) {
            poll.isPublicResults = pollCreateDto.isPublicResults
        }
        poll.participants = participated
        poll.options = pollCreateDto.options
        poll.startPollingTime = startTime
        poll.endPollingTime = endTime
        val category = optionalCategory.get()
        poll.category = category
        poll.createdPollTime = appTool.getNowTime()
        if (startTime.isAfter(appTool.getNowTime())) {
            poll.status = Status.PENDING.name
        }
        category.polls.add(poll)
        for (participate in participated) {
            participate.participatedPolls.add(poll)
        }
        val savedPoll = pollRepository.save(poll)

        // audit
        auditOperation("CREATE_POLL", parseToken.username, "", false, requestHeader)
        return convertToPollResponseDto(savedPoll)
    }

    fun participate(participatePollDto: ParticipatePollDto, requestHeader: HttpHeaders): ParticipatePollResponse {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        val optionalPoll = pollRepository.findById(participatePollDto.pollId)
        val userOptional = userRepository.findById(parseToken.id)
        val user: User = userOptional.get()
        if (optionalPoll.isEmpty) {
            throw ValidationDataException("No poll is founded with this pollId: ${participatePollDto.pollId}")
        }
        val poll = optionalPoll.get()
        if (poll.deleted) {
            throw ValidationDataException("This poll with this ${participatePollDto.pollId} is already deleted")
        }
        if (poll.status == Status.FINISHED.name) {
            throw ValidationDataException("The poll has ended and is no longer accepting responses")
        }
        if (poll.status == Status.PENDING.name) {
            throw ValidationDataException("This poll has not yet begun accepting responses")
        }
        if (poll.status == Status.CANCELED.name) {
            throw ValidationDataException("This poll has already canceled")
        }
        participatePollDto.selectedOptions = participatePollDto.selectedOptions.map { it.trim().toLowerCase() }.toSet()
        if (!poll.options.containsAll(participatePollDto.selectedOptions)) {
            throw ValidationDataException("This poll has those options: ${poll.options}. Please select from those options")
        }
        if (poll.type == PollType.SINGLE_CHOICE.name && participatePollDto.selectedOptions.size != 1) {
            throw ValidationDataException("This poll is just single choice. Select please one choice from the options")
        }
        val optionalResult: Optional<DuringPollingResult> =
            duringPollingResultRepository.findDuringPollingResultsByUserIdAndPollId(user.id, participatePollDto.pollId)
        if (optionalResult.isPresent) {
            trackingPollService.isAlreadyParticipateInPoll(user.id, optionalResult.get().timeOfPolling)
        }
        if (poll.visibility == VisibilityType.PRIVATE.name) {
            var contains = false
            for (participant in poll.participants) {
                if (participant.id == user.id) {
                    contains = true
                    break
                }
            }
            if (!contains) {
                throw ValidationDataException("This poll is private for particular Users, You don't belong to this Participant ")
            }
        }
        val duringPollingResult = DuringPollingResult()
        duringPollingResult.pollId = poll.id
        duringPollingResult.userId = user.id
        duringPollingResult.titleOfPoll = poll.title
        duringPollingResult.descriptionPoll = poll.description
        duringPollingResult.timeOfPolling = appTool.getNowTime()
        var options = ""
        for (str in participatePollDto.selectedOptions) {
            options += "$str, "
        }
        duringPollingResult.selectedOptions = options
        duringPollingResult.startTimeOfPolling = poll.startPollingTime
        duringPollingResult.endTimeOfPolling = poll.endPollingTime
        duringPollingResultRepository.save(duringPollingResult)

        // audit
        auditOperation("PARTICIPATE_IN_POLL", parseToken.username, "", false, requestHeader)

        val participatePollResponse = ParticipatePollResponse()
        participatePollResponse.pollId = poll.id
        participatePollResponse.selectedOptions = duringPollingResult.selectedOptions
        participatePollResponse.title = duringPollingResult.titleOfPoll
        participatePollResponse.description = duringPollingResult.descriptionPoll
        participatePollResponse.message =
            "You have participated in the polling ${duringPollingResult.titleOfPoll} at ${duringPollingResult.timeOfPolling}"

        return participatePollResponse
    }

    @Transactional
    fun updatePoll(pollId: Long, pollUpdateDto: PollDto, requestHeader: HttpHeaders): FullPollResponseDto {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        val optionalPoll: Optional<Poll> = pollRepository.findById(pollId)

        if (optionalPoll.isEmpty) {
            throw ValidationDataException("No poll is founded with this $pollId")
        }
        val poll = optionalPoll.get()
        val optionalUser = userRepository.findById(parseToken.id)
        if (!optionalUser.get().polls.contains(poll)) {
            throw ValidationDataException("This poll: ${poll.title} does not belong to you. You can't update it")
        }
        if (poll.deleted) {
            throw ValidationDataException("This poll with this $pollId is already deleted")
        }
        if (poll.status == Status.FINISHED.name) {
            throw ValidationDataException("The poll has been ended and you can't change it ")
        }
        if (poll.status == Status.OPEN_ACTIVE.name) {
            throw ValidationDataException("The poll has been opened and you can't change it ")
        }

        if (!Strings.isNullOrEmpty(pollUpdateDto.title)) {
            poll.title = pollUpdateDto.title
        }
        if (!Strings.isNullOrEmpty(pollUpdateDto.description)) {
            poll.description = pollUpdateDto.description
            if (optionalUser.get().polls.contains(poll)) {
                throw ValidationDataException("This poll with the title: ${pollUpdateDto.title} and description: ${pollUpdateDto.description} is already exist")
            }
        }

        if (pollUpdateDto.options.isNotEmpty()) {
            pollUpdateDto.options = pollUpdateDto.options.map { it.trim().toLowerCase() }.sorted().toSet()
            if (pollUpdateDto.options.isEmpty() || pollUpdateDto.options.size == 1) {
                throw ValidationDataException("A poll must have options and at list two options")
            }
            poll.options = pollUpdateDto.options
        }

        if (pollUpdateDto.participated != null && pollUpdateDto.participated!!.size < 5) {
            throw ValidationDataException("A poll with private visibility must have  at list five participated")
        }

        val participated: MutableSet<User> = mutableSetOf()
        val noUsers: MutableSet<Long> = mutableSetOf()
        var user = User()
        if (pollUpdateDto.participated != null) {
            for (item: Long in pollUpdateDto.participated!!) {
                val userOptional = userRepository.findById(item)
                user = userOptional.get()
                if (userOptional.isPresent) {
                    participated.add(user)
                } else {
                    noUsers.add(item)
                }
            }
            participated.add(user)
            if (participated.size < 5) {
                throw ValidationDataException("A poll with private visibility must have  at list five participated, Some of the given users are not found or deleted: $noUsers")
            }
            poll.visibility = VisibilityType.PRIVATE.name
            poll.participants = participated
        }
        val endPollingTime: ZonedDateTime?
        if (!Strings.isNullOrEmpty(pollUpdateDto.endPollingTime)) {
            if (!appTool.validateDate(pollUpdateDto.endPollingTime)) {
                throw ValidationDataException(" EndTime of the  poll must have such format YYYY-MM-DD HH:MM:SS")
            }
            endPollingTime = appTool.toRightDateFormat(pollUpdateDto.endPollingTime)
            if (endPollingTime.isBefore(appTool.getNowTime())) {
                throw ValidationDataException(" EndPollingTime of the  poll must be in future")
            }
        } else {
            endPollingTime = poll.endPollingTime
        }
        val startTime: ZonedDateTime?
        if (!Strings.isNullOrEmpty(pollUpdateDto.startPollingTime)) {
            if (!appTool.validateDate(pollUpdateDto.startPollingTime)) {
                throw ValidationDataException(" StartTime of the  poll must have such format YYYY-MM-DD HH:MM:SS")
            }
            startTime = appTool.toRightDateFormat(pollUpdateDto.startPollingTime)
            if (startTime.isBefore(appTool.getNowTime())) {
                throw ValidationDataException(" StartTime of the  poll must be in the present or future")
            }
        } else {
            startTime = poll.startPollingTime
        }
        if (endPollingTime!!.isBefore(startTime)) {
            throw ValidationDataException(" EndDateTime of a poll must be after startTime of Polling ")
        }
        if (Duration.between(startTime, endPollingTime).toHours() < 1) {
            throw ValidationDataException("A poll must have a period at least one hours")
        }
        poll.startPollingTime = startTime
        poll.endPollingTime = endPollingTime
        if (startTime!!.isAfter(appTool.getNowTime())) {
            poll.status = Status.PENDING.name
        }
        if (!Strings.isNullOrEmpty(pollUpdateDto.category)) {
            val optionalCategory = categoryService.findCategory(pollUpdateDto.category)
            if (optionalCategory.isEmpty) {
                throw ValidationDataException("This category ${pollUpdateDto.category} is no category: The categories are ${categoryService.getAllCategoriesName()}")
            }
            poll.category = optionalCategory.get()
        }
        if (pollUpdateDto.isPublicResults != null) {
            poll.isPublicResults = pollUpdateDto.isPublicResults
        }
        poll.updatedPollTime = appTool.getNowTime()

        // audit
        auditOperation("UPDATE_POLL", parseToken.username, "", false, requestHeader)
        return convertToPollResponseDto(poll)
    }

    @Transactional
    fun finishPoll(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): String {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkControlPollDto(controlPollDto)
        if(controlPollDto.isAdmin){
            checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        }
        val pollId = controlPollDto.pollingId

        val optionalPoll: Optional<Poll> = pollRepository.findById(pollId!!)
        checkPoll(optionalPoll, pollId)

        val poll = optionalPoll.get()

        if (!controlPollDto.isAdmin) {
            val optionalUser = userRepository.findById(parseToken.id)
            if (!optionalUser.get().polls.contains(poll)) {
                throw ValidationDataException("This poll: ${poll.title} does not belong to you. You can't finish  it")
            }
        }
        if (poll.deleted) {
            throw ValidationDataException("This poll with this $pollId is already deleted")
        }
        if (poll.status == Status.FINISHED.name) {
            throw ValidationDataException("The poll has been already ended  ")
        }
        if (poll.status == Status.PENDING.name || poll.status == Status.CANCELED.name) {
            throw ValidationDataException("This poll could be not finished, cause it is ${poll.status}")
        }
        poll.status = Status.FINISHED.name
        if (duringPollingResultRepository.findAll().filter { it.pollId == optionalPoll.get().id }.toSet()
                .isNotEmpty()
        ) {
            poll.resultState = ResultPollingStates.PENDING_RESULT.name
        } else {
            poll.resultState = ResultPollingStates.COMPLETED.name
        }

        // audit
        auditOperation("FINISH_POLL", parseToken.username, controlPollDto.reason, controlPollDto.isAdmin, requestHeader)
        return Status.FINISHED.name
    }

    @Transactional
    fun openPoll(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): String {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkControlPollDto(controlPollDto)
        if(controlPollDto.isAdmin){
            checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        }

        val pollId = controlPollDto.pollingId

        val optionalPoll: Optional<Poll> = pollRepository.findById(pollId!!)
        checkPoll(optionalPoll, pollId)

        val poll = optionalPoll.get()

        if (!controlPollDto.isAdmin) {
            val optionalUser = userRepository.findById(parseToken.id)
            if (!optionalUser.get().polls.contains(poll)) {
                throw ValidationDataException("This poll: ${poll.title} does not belong to you. You can't open  it")
            }
        }

        if (poll.deleted) {
            throw ValidationDataException("This poll with this $pollId is already deleted")
        }
        if (poll.status == Status.FINISHED.name) {
            throw ValidationDataException("The poll has been already ended  ")
        }
        if (poll.status == Status.OPEN_ACTIVE.name) {
            throw ValidationDataException("The poll has been already opened  ")
        }
        poll.status = Status.OPEN_ACTIVE.name
        poll.startPollingTime = appTool.getNowTime()

        // audit
        auditOperation("OPEN_POLL", parseToken.username, controlPollDto.reason, controlPollDto.isAdmin, requestHeader)
        return Status.OPEN_ACTIVE.name
    }

    @Transactional
    fun cancelPoll(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): String {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkControlPollDto(controlPollDto)
        if(controlPollDto.isAdmin){
            checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        }

        val pollId = controlPollDto.pollingId

        val optionalPoll: Optional<Poll> = pollRepository.findById(pollId!!)
        checkPoll(optionalPoll, pollId)

        val poll = optionalPoll.get()

        if (!controlPollDto.isAdmin) {
            val optionalUser = userRepository.findById(parseToken.id)
            if (!optionalUser.get().polls.contains(poll)) {
                throw ValidationDataException("This poll: ${poll.title} does not belong to you. You can't open  it")
            }
        }

        if (poll.deleted) {
            throw ValidationDataException("This poll with this $pollId is already deleted")
        }
        if (poll.status == Status.FINISHED.name) {
            throw ValidationDataException("The poll has been already ended  ")
        }
        if (poll.status == Status.CANCELED.name) {
            throw ValidationDataException("The poll has been already canceled  ")
        }

        poll.status = Status.CANCELED.name

        // audit
        auditOperation("CANCEL_POLL", parseToken.username, controlPollDto.reason, controlPollDto.isAdmin, requestHeader)
        return Status.CANCELED.name
    }

    @Transactional
    fun pendingPoll(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): String {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkControlPollDto(controlPollDto)
        if(controlPollDto.isAdmin){
            checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        }

        val pollId = controlPollDto.pollingId

        val optionalPoll: Optional<Poll> = pollRepository.findById(pollId!!)
        checkPoll(optionalPoll, pollId)

        val poll = optionalPoll.get()

        if (!controlPollDto.isAdmin) {
            val optionalUser = userRepository.findById(parseToken.id)
            if (!optionalUser.get().polls.contains(poll)) {
                throw ValidationDataException("This poll: ${poll.title} does not belong to you. You can't open  it")
            }
        }
        if (poll.deleted) {
            throw ValidationDataException("This poll with this $pollId is already deleted")
        }
        if (poll.status == Status.FINISHED.name) {
            throw ValidationDataException("The poll has been already ended  ")
        }
        if (poll.status == Status.PENDING.name) {
            throw ValidationDataException("The poll has been already pended  ")
        }
        if (Strings.isNullOrEmpty(controlPollDto.endPollingTime)) {
            throw ValidationDataException("to pending a poll must have an End Datetime ")
        }
        if (!appTool.validateDate(controlPollDto.endPollingTime)) {
            throw ValidationDataException(" EndTime of the  poll must have such format YYYY-MM-DD HH:MM:SS")
        }
        if (Strings.isNullOrEmpty(controlPollDto.startPollingTime)) {
            throw ValidationDataException("to pending a poll must have start Datetime ")
        }
        if (!appTool.validateDate(controlPollDto.startPollingTime)) {
            throw ValidationDataException(" StartTime of the  poll must have such format YYYY-MM-DD HH:MM:SS")
        }
        val startTime: ZonedDateTime = appTool.toRightDateFormat(controlPollDto.startPollingTime)
        if (startTime.isBefore(appTool.getNowTime())) {
            throw ValidationDataException("To pending a poll must the given star Datetime bigger than the time of now ")
        }
        val endTime: ZonedDateTime = appTool.toRightDateFormat(controlPollDto.endPollingTime)
        if (endTime.isBefore(startTime)) {
            throw ValidationDataException(" EndDateTime of a poll must be after startTime of Polling ")
        }
        if (Duration.between(startTime, endTime).toHours() < 1) {
            throw ValidationDataException("A poll must have a period at least one hours")
        }
        poll.status = Status.PENDING.name
        poll.startPollingTime = startTime
        poll.endPollingTime = endTime

        // audit
        auditOperation(
            "PENDING_POLL",
            parseToken.username,
            controlPollDto.reason,
            controlPollDto.isAdmin,
            requestHeader
        )

        return Status.PENDING.name
    }

    fun getCreatedPollsOfUser(
        responseType: String,
        status: String,
        statusResult: String,
        requestHeader: HttpHeaders
    ): Set<Any> {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        blackListService.checkTokenOrUsernameInBlackList(parseToken.username, requestHeader, null)
        val optionalUser = userRepository.findById(parseToken.id)
        val user = optionalUser.get()
        val setOfStatus: Set<String> = setOf(
            Status.PENDING.name,
            Status.FINISHED.name,
            Status.OPEN_ACTIVE.name,
            Status.ARCHIVED.name,
            Status.CANCELED.name
        )
        val setOfStatusResult = setOf(
            ResultPollingStates.PENDING_RESULT.name,
            ResultPollingStates.COMPLETED.name,
            ResultPollingStates.AWAITING.name
        )
        if (statusResult != "None" && !Strings.isNullOrEmpty(statusResult)) {
            if (!setOfStatusResult.contains(statusResult)) {
                throw ValidationDataException("Please give status of result from this set $setOfStatusResult")
            }
            if (responseType == "Small") {
                return user.polls.filter { it.resultState == statusResult && it.status != Status.CANCELED.name && !it.deleted }
                    .map { convertToSmallPollResponseDto(it) }
                    .toSet()
            }
            return user.polls.filter { it.resultState == statusResult && it.status != Status.CANCELED.name && !it.deleted }
                .map { convertToPollResponseDto(it) }
                .toSet()
        }
        if (statusResult != "None" && !Strings.isNullOrEmpty(status)) {
            if (!setOfStatus.contains(status)) {
                throw ValidationDataException("Please give status from this set $setOfStatus")
            }
            if (responseType == "Small") {
                return user.polls
                    .filter { it.status == status && !it.deleted }.map { convertToSmallPollResponseDto(it) }.toSet()
            }
            return user.polls
                .filter { it.status == status && !it.deleted }.map { convertToPollResponseDto(it) }.toSet()
        }
        if (responseType == "Small") {
            return user.polls.filter { !it.deleted && it.status != Status.CANCELED.name }
                .map { convertToSmallPollResponseDto(it) }.toSet()
        }
        return user.polls.filter { !it.deleted && it.status != Status.CANCELED.name }
            .map { convertToPollResponseDto(it) }.toSet()
    }

    fun getAllPolls(
        responseType: String,
        status: String,
        statusResult: String,
        requestHeader: HttpHeaders
    ): Set<Any> {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        blackListService.checkTokenOrUsernameInBlackList(parseToken.username, requestHeader, null)
        val setOfStatus: Set<String> = setOf(Status.PENDING.name, Status.FINISHED.name, Status.OPEN_ACTIVE.name)

        val setOfStatusResult = setOf(
            ResultPollingStates.PENDING_RESULT.name,
            ResultPollingStates.COMPLETED.name,
            ResultPollingStates.AWAITING.name
        )
        if (!Strings.isNullOrEmpty(statusResult) && statusResult != "None") {
            if (!setOfStatusResult.contains(statusResult)) {
                throw ValidationDataException("Please give status of result from this set $setOfStatusResult")
            }
            if (responseType == "Small") {
                return pollRepository.findAll()
                    .filter {
                        it.resultState == statusResult && it.status != Status.CANCELED.name && !it.deleted && isPollVisibleForAll(
                            it,
                            parseToken.id
                        )
                    }
                    .map { convertToSmallPollResponseDto(it) }.toSet()
            }
            return pollRepository.findAll()
                .filter {
                    it.resultState == statusResult && it.status != Status.CANCELED.name && !it.deleted && isPollVisibleForAll(
                        it,
                        parseToken.id
                    )
                }
                .map { convertToPollResponseDto(it) }.toSet()
        }
        if (!Strings.isNullOrEmpty(status) && status != "None") {
            if (!setOfStatus.contains(status)) {
                throw ValidationDataException("Please give status from this set $setOfStatus")
            }
            if (responseType == "Small") {
                return pollRepository.findAll()
                    .filter {
                        it.status == status && it.status != Status.CANCELED.name && !it.deleted && isPollVisibleForAll(
                            it,
                            parseToken.id
                        )
                    }
                    .map { convertToSmallPollResponseDto(it) }.toSet()
            }
            return pollRepository.findAll()
                .filter {
                    it.status == status && it.status != Status.CANCELED.name && !it.deleted && isPollVisibleForAll(
                        it,
                        parseToken.id
                    )
                }
                .map { convertToPollResponseDto(it) }.toSet()
        }
        if (responseType == "Small") {
            return pollRepository.findAll().filter {
                it.status != Status.CANCELED.name && !it.deleted && isPollVisibleForAll(
                    it,
                    parseToken.id
                )
            }
                .map { convertToSmallPollResponseDto(it) }.toSet()
        }
        return pollRepository.findAll().filter {
            it.status != Status.CANCELED.name && !it.deleted && isPollVisibleForAll(
                it,
                parseToken.id
            )
        }
            .map { convertToPollResponseDto(it) }.toSet()
    }

    fun showPollUsingPollId(responseType: String, pollId: Long, requestHeader: HttpHeaders): Any {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        blackListService.checkTokenOrUsernameInBlackList(parseToken.username, requestHeader, null)
        val optionalPoll = pollRepository.findById(pollId)
        if (optionalPoll.isEmpty) {
            throw ValidationDataException("This poll with pollId: $pollId is not found")
        }
        val poll = optionalPoll.get()
        if (poll.deleted) {
            throw ValidationDataException("This poll with pollId: $pollId is deleted")
        }
        if (!isPollVisibleForAll(poll, parseToken.id)) {
            throw ValidationDataException("This poll: $pollId is a private poll and its result is not public for all. It is just for its Participants")
        }
        if (responseType == "small") {
            return convertToSmallPollResponseDto(poll)
        }
        return convertToPollResponseDto(poll)
    }

    fun showResultOfPolling(pollId: Long, userId: Long, requestHeader: HttpHeaders): PollingResult {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        blackListService.checkTokenOrUsernameInBlackList(parseToken.username, requestHeader, null)
        return pollingResultService.getResultOfPoll(pollId, userId)
    }

    fun deletePollById(controlPollDto: ControlPollDto, requestHeader: HttpHeaders): Boolean {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkControlPollDto(controlPollDto)
        if(controlPollDto.isAdmin){
            checkTokenOrUsernameInBlackList(parseToken.username, requestHeader)
        }

        val pollId = controlPollDto.pollingId

        val optionalPoll: Optional<Poll> = pollRepository.findById(pollId!!)
        checkPoll(optionalPoll, pollId)

        val poll = optionalPoll.get()
        val optionalUser = userRepository.findById(parseToken.id)
        if (!optionalUser.get().polls.contains(poll)) {
            throw ValidationDataException("This poll: ${poll.title} does not belong to you. You can't delete it")
        }
        if (poll.deleted) {
            throw ValidationDataException("This poll with this $pollId is already deleted")
        }
        poll.deleted = true
        pollRepository.save(poll)

        // audit
        auditOperation("DELETE_POLL", parseToken.username, controlPollDto.reason, controlPollDto.isAdmin, requestHeader)
        return true
    }

    fun rePolling(rePollingDto: RePollingDto, requestHeader: HttpHeaders): FullPollResponseDto {
        val parseToken: ParsTokenResponse = jwtTool.parseTokenResponse
        checkTokenOrUsernameInBlackList(parseToken.username,requestHeader)
        if (rePollingDto.pollId?.toInt() == 0) {
            throw ValidationDataException("You have to give a poll Id, which is already completed ")
        }

        val optionalPoll = pollRepository.findById(rePollingDto.pollId)
        if (optionalPoll.isEmpty) {
            throw ValidationDataException("This poll with ${rePollingDto.pollId} is not found")
        }
        val poll = optionalPoll.get()
        if (poll.deleted) {
            throw ValidationDataException("This poll with ${rePollingDto.pollId} is already deleted")
        }
        if (poll.resultState != "COMPLETED") {
            throw ValidationDataException("This poll must be already completed")
        }
        if (poll.visibility == VisibilityType.PRIVATE.name) {
            val participants = poll.participants
            var notFound = false
            for (u in participants) {
                if (u.id == parseToken.id) {
                    notFound = true
                    break
                }
            }
            if (!notFound) {
                throw ValidationDataException("This user with ${parseToken.id} can not redeploy this poll. It is a private Poll")
            }
        }

        if (Strings.isNullOrEmpty(rePollingDto.newEndTimeOfPolling)) {
            throw ValidationDataException(" You have to give a new EndTime to repoll this poll ")
        }
        if (!appTool.validateDate(rePollingDto.newEndTimeOfPolling)) {
            throw ValidationDataException(" EndTime of the  poll must have such format YYYY-MM-DD HH:MM:SS")
        }
        val newEndTime: ZonedDateTime?
        newEndTime = appTool.toRightDateFormat(rePollingDto.newEndTimeOfPolling)
        if (newEndTime.isBefore(appTool.getNowTime())) {
            throw ValidationDataException(" EndTime of the  poll must be in the present or future")
        }

        val newStartTime: ZonedDateTime?
        if (!Strings.isNullOrEmpty(rePollingDto.newStartTimeOfPolling)) {
            if (!appTool.validateDate(rePollingDto.newStartTimeOfPolling)) {
                throw ValidationDataException(" StartTime of the  poll must have such format YYYY-MM-DD HH:MM:SS")
            }
            newStartTime = appTool.toRightDateFormat(rePollingDto.newStartTimeOfPolling)
            if (newStartTime.isBefore(appTool.getNowTime())) {
                throw ValidationDataException(" StartTime of the  poll must be in the present or future")
            }
        } else {
            newStartTime = appTool.getNowTime()
        }
        if (newEndTime.isBefore(newStartTime)) {
            throw ValidationDataException(" EndDateTime of a poll must be after startTime of Polling ")
        }
        if (Duration.between(newStartTime, newEndTime).toHours() < 1) {
            throw ValidationDataException("A poll must have a period at least one hours")
        }
        poll.startPollingTime = newStartTime
        poll.endPollingTime = newEndTime
        poll.resultState = ResultPollingStates.AWAITING.name
        if (newStartTime.isAfter(appTool.getNowTime())) {
            poll.status = Status.PENDING.name
        } else {
            poll.status = Status.OPEN_ACTIVE.name
        }
        poll.updatedPollTime = appTool.getNowTime()
        val savedRePoll = pollRepository.save(poll)

        // audit
        auditOperation("RE_POLL", parseToken.username, "", false, requestHeader)
        return convertToPollResponseDto(savedRePoll)
    }

    private fun convertToPollResponseDto(poll: Poll): FullPollResponseDto {

        val pollResponseDto = FullPollResponseDto()
        pollResponseDto.pollType = poll.type
        pollResponseDto.title = poll.title
        pollResponseDto.pollId = poll.id
        pollResponseDto.options = poll.options
        pollResponseDto.startPollingTime = poll.startPollingTime.toString()
        pollResponseDto.endPollingTime = poll.endPollingTime.toString()
        pollResponseDto.visibility = poll.visibility
        pollResponseDto.participated = userService.allUsersAsSmallUserDto(poll.participants)
        pollResponseDto.description = poll.description
        pollResponseDto.category = poll.category.categoryName
        pollResponseDto.status = poll.status
        pollResponseDto.isPublicResults = poll.isPublicResults
        pollResponseDto.creator = poll.creator!!.username
        pollResponseDto.resultState = poll.resultState
        return pollResponseDto
    }

    private fun convertToSmallPollResponseDto(poll: Poll): SmallPollResponseDto {
        val smallPollResponseDto = SmallPollResponseDto()
        smallPollResponseDto.pollId = poll.id
        smallPollResponseDto.title = poll.title
        smallPollResponseDto.options = poll.options
        smallPollResponseDto.description = poll.description
        smallPollResponseDto.status = poll.status
        smallPollResponseDto.creator = poll.creator!!.username
        return smallPollResponseDto
    }


    private fun isPollVisibleForAll(poll: Poll, userId: Long): Boolean {

        val participants = poll.participants
        for (participant in participants) {
            if (participant.id == userId) {
                return true
            }
        }
        return poll.visibility == VisibilityType.PUBLIC.name
    }

    private fun checkTokenOrUsernameInBlackList(username: String?, requestHeader: HttpHeaders) {
        blackListService.checkTokenOrUsernameInBlackList(username, requestHeader, null)
    }

    private fun checkControlPollDto(controlPollDto: ControlPollDto) {
        if (controlPollDto.pollingId == null) {
            throw ValidationDataException("PollingId is null")
        }
        if (Strings.isNullOrEmpty(controlPollDto.reason)) {
            throw ValidationDataException("The request must have a reason to do the operation")
        }
    }

    private fun checkPoll(optionalPoll: Optional<Poll>, pollId: Long) {
        if (optionalPoll.isEmpty) {
            throw ValidationDataException("No poll is not founded with this $pollId")
        }
    }

    private fun auditOperation(
        action: String,
        username: String?,
        reason: String,
        isAdmin: Boolean,
        requestHeader: HttpHeaders
    ) {
        val ipAddress = appTool.getUerIpAddress(requestHeader)
        if (isAdmin) {
            adminAuditService.saveAction(action, username!!, ipAddress, reason, appTool.getNowTime(), null)
        }
        userAuditService.saveAction(
            action,
            username!!,
            ipAddress,
            reason,
            appTool.getNowTime(),
            null
        )
    }

}