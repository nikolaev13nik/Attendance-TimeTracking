package att.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import att.api.TimeTrackingApi;
import att.context.AsyncMessageHandler;
import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.dto.EditDataTimeUserDto;
import att.dto.EditLeaveDayDto;
import att.dto.LeaveDayDto;
import att.dto.LeaveReportRequestDto;
import att.dto.MonthlyUserStatisticInfoDto;
import att.dto.SessionDataDto;
import att.security.CallerResolver;
import att.service.strategy.AddLeaveDaysService;
import att.service.strategy.CheckNullRowsService;
import att.service.strategy.CloseSessionService;
import att.service.strategy.CountWorkedDaysService;
import att.service.strategy.GetLeaveDaysService;
import att.service.strategy.GetOvertimeMinutesBetweenService;
import att.service.strategy.GetRecordsByRangeService;
import att.service.strategy.GetWorkedMinutesBetweenService;
import att.service.strategy.LeaveDayChangeService;
import att.service.strategy.LeaveDayRemoveService;
import att.service.strategy.OpenSessionService;
import att.service.strategy.RemoveRecordService;
import att.service.strategy.SessionChangeService;
import att.service.strategy.StatisticInfoService;
import co.il.attendance.security.AdminOnly;
import co.il.attendance.security.AdminOrSelf;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AttendanceTimeTrackingController implements TimeTrackingApi {

    private final OpenSessionService openSessionService;
    private final CloseSessionService closeSessionService;
    private final SessionChangeService sessionChangeService;
    private final CountWorkedDaysService countWorkedDaysService;
    private final GetRecordsByRangeService getRecordsByRangeService;
    private final RemoveRecordService removeRecordService;
    private final GetWorkedMinutesBetweenService getWorkedMinutesBetweenService;
    private final GetOvertimeMinutesBetweenService getOvertimeMinutesBetweenService;
    private final CheckNullRowsService checkNullRowsService;
    private final StatisticInfoService statisticInfoService;
    private final AddLeaveDaysService addLeaveDaysService;
    private final GetLeaveDaysService getLeaveDaysService;
    private final LeaveDayChangeService leaveDayChangeService;
    private final LeaveDayRemoveService leaveDayRemoveService;
    private final AsyncMessageHandler statisticAsyncMessageHandler;

    @AdminOrSelf
    @Override
    public ResponseEntity<DataTimeDto> openSession(@PathVariable Integer tenantId, @PathVariable Integer idUser,
                                                   @RequestBody SessionDataDto sessionDataDto) {
        DataTimeContext<SessionDataDto> context = DataTimeContext.<SessionDataDto>builder().idUser(idUser)
                .task(sessionDataDto).openSessionDate(sessionDataDto.getOpenSessionDate())
                .tenantId(tenantId).workDate(sessionDataDto.getWorkDate()).build();
        openSessionService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseDataTimeDto());
    }

    @AdminOrSelf
    @Override
    public ResponseEntity<DataTimeDto> closeSession(@PathVariable Integer tenantId, @PathVariable Integer idUser,
                                                    @RequestBody SessionDataDto sessionDataDto) {
        DataTimeContext<SessionDataDto> context = DataTimeContext.<SessionDataDto>builder().idUser(idUser)
                .closeSessionDate(sessionDataDto.getCloseSessionDate()).tenantId(tenantId).task(sessionDataDto)
                .workDate(sessionDataDto.getWorkDate()).build();
        closeSessionService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseDataTimeDto());
    }

    @AdminOnly
    @Override
    public ResponseEntity<DataTimeDto> editRecord(@PathVariable Integer tenantId,
                                                  @RequestBody EditDataTimeUserDto dataTimeDto) {
        DataTimeContext<EditDataTimeUserDto> context = DataTimeContext.<EditDataTimeUserDto>builder()
                .editDto(dataTimeDto).tenantId(tenantId).build();
        sessionChangeService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseDataTimeDto());
    }

    @AdminOrSelf
    @Override
    public ResponseEntity<List<DataTimeDto>> getRecordsEmployeeByRange(@PathVariable Integer tenantId,
                                                                       @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                                       @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
                                                                       @RequestParam Integer idUser) {
        DataTimeContext<Void> context = DataTimeContext.<Void>builder().idUser(idUser).startDate(startDate)
                .tenantId(tenantId)
                .endDate(endDate).build();
        getRecordsByRangeService.execute(context);
        return ResponseEntity.ok(context.getResponseDataTimeDto());
    }

    @AdminOnly
    @Override
    public ResponseEntity<Long> getCountWorkedDaysByEmployee(@PathVariable Integer tenantId,
                                                             @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                             @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
                                                             @RequestParam Integer idUser) {
        DataTimeContext<Void> context = DataTimeContext.<Void>builder().idUser(idUser).startDate(startDate)
                .endDate(endDate).tenantId(tenantId).build();
        countWorkedDaysService.execute(context);
        return ResponseEntity.ok(context.getTotalDaysPerUser().get(idUser));
    }

    @AdminOnly
    @Override
    public ResponseEntity<Long> getAllMinutesEmployeeBetweenDates(@PathVariable Integer tenantId,
                                                                @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                                @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
                                                                @RequestParam Integer idUser) {
        DataTimeContext<Void> context = DataTimeContext.<Void>builder().idUser(idUser).startDate(startDate)
                .tenantId(tenantId)
                .endDate(endDate).build();
        getWorkedMinutesBetweenService.execute(context);
        return ResponseEntity.ok(context.getTotalWorkMinutesPerUser().get(idUser));
    }

    @AdminOnly
    @Override
    public ResponseEntity<Long> getOvertimeMinutesEmployeeBetweenDates(@PathVariable Integer tenantId,
                                                                @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                                @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
                                                                @RequestParam Integer idUser) {
        DataTimeContext<Void> context = DataTimeContext.<Void>builder().idUser(idUser).startDate(startDate)
                .endDate(endDate).tenantId(tenantId).build();
        getOvertimeMinutesBetweenService.execute(context);
        return ResponseEntity.ok(context.getTotalOvertimeMinutesPerUser().get(idUser));
    }

    @AdminOrSelf
    @Override
    public ResponseEntity<Void> addLeaveDays(@PathVariable Integer tenantId, @PathVariable Integer idUser,
                                             @RequestBody LeaveReportRequestDto leaveReport) {
        DataTimeContext<LeaveReportRequestDto> context = DataTimeContext.<LeaveReportRequestDto>builder().idUser(idUser)
                .task(leaveReport)
                .tenantId(tenantId).build();
        addLeaveDaysService.execute(context);
        return ResponseEntity.ok().build();
    }

    @AdminOrSelf
    @Override
    public ResponseEntity<List<DataTimeDto>> checkRowsForNull(@PathVariable Integer tenantId,
                                                              @PathVariable Integer idUser,
                                                              @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                              @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
        DataTimeContext<List<DataTimeDto>> context =
                DataTimeContext.<List<DataTimeDto>>builder().idUser(idUser).tenantId(tenantId)
                        .startDate(startDate).endDate(endDate).build();
        checkNullRowsService.execute(context);
        return ResponseEntity.ok(context.getResponseDataTimeDto());
    }

    @AdminOnly
    @Override
    public ResponseEntity<List<MonthlyUserStatisticInfoDto>> getMonthStatistic(@PathVariable Integer tenantId,
                                                                               @RequestParam YearMonth targetMonth,
                                                                               @RequestParam(required = false) Integer idUser,
                                                                               @RequestParam(required = false, defaultValue = "false")
                                                                                   Boolean report) {
        DataTimeContext<Void> context = DataTimeContext.<Void>builder()
                .idUser(idUser).tenantId(tenantId)
                .reporterId(CallerResolver.currentUserId()).reporterTenantId(CallerResolver.currentHomeTenantId())
                .build();
        context.setAsyncMessageHandler(statisticAsyncMessageHandler);
        context.getStatisticInfoHolder().setReport(report);
        context.getStatisticInfoHolder().setStartOfMonth(targetMonth.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        statisticInfoService.execute(context);
        context.getPostServiceAction().accept(context);
        return ResponseEntity.ok(context.getStatisticInfoHolder().getMultipleUserData().values().stream().toList());
    }

    @AdminOrSelf
    @Override
    public ResponseEntity<List<LeaveDayDto>> getLeaveDays(@PathVariable Integer tenantId,
                                                          @PathVariable Integer idUser,
                                                          @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                          @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
        DataTimeContext<Void> context = DataTimeContext.<Void>builder().idUser(idUser).tenantId(tenantId)
                .startDate(startDate).endDate(endDate).build();
        getLeaveDaysService.execute(context);
        return ResponseEntity.ok(context.getResponseLeaveDayDto());
    }

    @AdminOnly
    @Override
    public ResponseEntity<DataTimeDto> removeRecord(@PathVariable Integer tenantId, @PathVariable Integer id) {
        DataTimeContext<DataTimeDto> context =
                DataTimeContext.<DataTimeDto>builder().recordId(id).tenantId(tenantId).build();
        removeRecordService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseDataTimeDto());
    }

    @AdminOnly
    @Override
    public ResponseEntity<LeaveDayDto> editLeaveDay(@PathVariable Integer tenantId,
                                                    @RequestBody EditLeaveDayDto editLeaveDayDto) {
        DataTimeContext<EditLeaveDayDto> context = DataTimeContext.<EditLeaveDayDto>builder()
                .editLeaveDayDto(editLeaveDayDto).tenantId(tenantId).build();
        leaveDayChangeService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseLeaveDayDto());
    }

    @AdminOnly
    @Override
    public ResponseEntity<LeaveDayDto> removeLeaveDay(@PathVariable Integer tenantId, @PathVariable Integer id) {
        DataTimeContext<LeaveDayDto> context =
                DataTimeContext.<LeaveDayDto>builder().leaveDayId(id).tenantId(tenantId).build();
        leaveDayRemoveService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseLeaveDayDto());
    }


}