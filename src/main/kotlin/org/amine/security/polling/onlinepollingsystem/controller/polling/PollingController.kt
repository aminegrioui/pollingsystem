package org.amine.security.polling.onlinepollingsystem.controller.polling

import org.amine.security.polling.onlinepollingsystem.dtos.poll.request.*
import org.amine.security.polling.onlinepollingsystem.dtos.poll.response.ParticipatePollResponse
import org.amine.security.polling.onlinepollingsystem.dtos.poll.response.FullPollResponseDto
import org.amine.security.polling.onlinepollingsystem.models.polling.PollingResult
import org.amine.security.polling.onlinepollingsystem.services.polling.PollingService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/polling/v1/polls")
class PollingController(val pollingService: PollingService) {


    @PostMapping("/create")
//    @PreAuthorize("hasAuthority('WHRITE_POLE')")
    fun createNewPoll(
        @RequestBody pollCreateDto: PollDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<FullPollResponseDto> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pollingService.createNewPoll(pollCreateDto, requestHeader))
    }

    @PostMapping("/participate")
//    @PreAuthorize("hasAuthority('PARTICIPATE_POLE')")
    fun participateInPoll(
        @RequestBody participatePollDto: ParticipatePollDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<ParticipatePollResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pollingService.participate(participatePollDto, requestHeader))
    }

    @PutMapping("/update/{id}")
//    @PreAuthorize("hasAuthority('WHRITE_POLE')")
    fun updatePoll(
        @RequestBody pollUpdateDto: PollDto,
        @PathVariable id: Long,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<FullPollResponseDto> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pollingService.updatePoll(id, pollUpdateDto, requestHeader))
    }

    @PutMapping("/finish/poll")
//    @PreAuthorize("hasAuthority('WHRITE_POLE')")
    fun finishPoll(
        @RequestBody controlPollDto: ControlPollDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pollingService.finishPoll(controlPollDto, requestHeader))
    }

    @PutMapping("/open/poll")
//    @PreAuthorize("hasAuthority('WHRITE_POLE')")
    fun openPoll(
        @RequestBody controlPollDto: ControlPollDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pollingService.openPoll(controlPollDto, requestHeader))
    }

    @PutMapping("/cancel/poll")
//    @PreAuthorize("hasAuthority('WHRITE_POLE')")
    fun cancelPoll(
        @RequestBody controlPollDto: ControlPollDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pollingService.cancelPoll(controlPollDto, requestHeader))
    }

    @PostMapping("/pending/poll")
//    @PreAuthorize("hasAuthority('WHRITE_POLE')")
    fun pendingPoll(
        @RequestBody controlPollDto: ControlPollDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pollingService.pendingPoll(controlPollDto, requestHeader))
    }

    @GetMapping("/poll/{responseType}/{pollId}")
    fun getPollUsingId(
        @PathVariable pollId: Long,
        @PathVariable responseType: String,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.OK)
            .body(pollingService.showPollUsingPollId(responseType, pollId, requestHeader))
    }

    @GetMapping("/{responseType}/{status}/{statusResult}")
    fun getAllPolls(
        @PathVariable status: String,
        @PathVariable statusResult: String,
        @PathVariable responseType: String,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<Set<Any>> {
        return ResponseEntity.status(HttpStatus.OK)
            .body(pollingService.getAllPolls(responseType, status, statusResult, requestHeader))
    }

    @GetMapping("/user/{responseType}/{status}/{statusResult}")
    fun getCreatedPollsOfUser(
        @PathVariable status: String,
        @PathVariable statusResult: String,
        @PathVariable responseType: String,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<Set<Any>> {
        return ResponseEntity.status(HttpStatus.OK)
            .body(pollingService.getCreatedPollsOfUser(responseType, status, statusResult, requestHeader))
    }

    @GetMapping("/poll/result/{pollId}/{userId}")
    fun showResultOfPolling(
        @PathVariable pollId: Long,
        @PathVariable userId: Long,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<PollingResult> {
        return ResponseEntity.status(HttpStatus.OK)
            .body(pollingService.showResultOfPolling(pollId, userId, requestHeader))
    }

    @DeleteMapping("/delete/poll")
//    @PreAuthorize("hasAuthority('WHRITE_POLE')")
    fun deletePollById(
        @RequestBody controlPollDto: ControlPollDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<Boolean> {
        return ResponseEntity.status(HttpStatus.OK)
            .body(pollingService.deletePollById(controlPollDto, requestHeader))
    }

    @PutMapping("/rePoll")
//    @PreAuthorize("hasAuthority('PARTICIPATE_POLE')")
    fun rePolling(
        @RequestBody rePollingDto: RePollingDto,
        @RequestHeader requestHeader: HttpHeaders
    ): ResponseEntity<FullPollResponseDto> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(pollingService.rePolling(rePollingDto, requestHeader))
    }
}