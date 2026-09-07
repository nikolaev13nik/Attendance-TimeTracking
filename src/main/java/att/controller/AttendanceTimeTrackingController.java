package att.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import att.api.TimeTrackingApi;
import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.dto.EditDataTimeUserDto;
import att.dto.LeaveReportRequestDto;
import att.dto.MonthlyUserStatisticInfoDto;
import att.dto.SessionDataDto;
import att.service.strategy.AddLeaveDaysService;
import att.service.strategy.CheckNullRowsService;
import att.service.strategy.CloseSessionService;
import att.service.strategy.CountWorkedDaysService;
import att.service.strategy.GetHoursBetweenService;
import att.service.strategy.GetOvertimeBetweenService;
import att.service.strategy.GetRecordsByRangeService;
import att.service.strategy.OpenSessionService;
import att.service.strategy.RemoveRecordService;
import att.service.strategy.SessionChangeService;
import att.service.strategy.StatisticInfoService;
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
    private final GetHoursBetweenService getHoursBetweenService;
    private final GetOvertimeBetweenService getOvertimeBetweenService;
    private final CheckNullRowsService checkNullRowsService;
    private final StatisticInfoService statisticInfoService;
    private final AddLeaveDaysService addLeaveDaysService;

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name()) || #idUser.toString() == authentication.name")
    public ResponseEntity<DataTimeDto> openSession(@PathVariable Integer tenantId, @PathVariable Integer idUser,
                                                   @RequestBody SessionDataDto sessionDataDto) {
        DataTimeContext<SessionDataDto> context = DataTimeContext.<SessionDataDto>builder().idUser(idUser)
                .task(sessionDataDto).openSessionDate(sessionDataDto.getOpenSessionDate())
                .tenantId(tenantId).workDate(sessionDataDto.getWorkDate()).build();
        openSessionService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseDataTimeDto());
    }

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name()) || #idUser.toString() == authentication.name")
    public ResponseEntity<DataTimeDto> closeSession(@PathVariable Integer tenantId, @PathVariable Integer idUser,
                                                    @RequestBody SessionDataDto sessionDataDto) {
        DataTimeContext<SessionDataDto> context = DataTimeContext.<SessionDataDto>builder().idUser(idUser)
                .closeSessionDate(sessionDataDto.getCloseSessionDate()).tenantId(tenantId).task(sessionDataDto)
                .workDate(sessionDataDto.getWorkDate()).build();
        closeSessionService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseDataTimeDto());
    }

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
    public ResponseEntity<DataTimeDto> editRecord(@PathVariable Integer tenantId,
                                                  @RequestBody EditDataTimeUserDto dataTimeDto) {
        DataTimeContext<EditDataTimeUserDto> context = DataTimeContext.<EditDataTimeUserDto>builder()
                .editDto(dataTimeDto).build();
        sessionChangeService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseDataTimeDto());
    }

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
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

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
    public ResponseEntity<Long> getCountWorkedDaysByEmployee(@PathVariable Integer tenantId,
                                                             @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                             @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
                                                             @RequestParam Integer idUser) {
        DataTimeContext<Void> context = DataTimeContext.<Void>builder().idUser(idUser).startDate(startDate)
                .endDate(endDate).tenantId(tenantId).build();
        countWorkedDaysService.execute(context);
        return ResponseEntity.ok(context.getTotalDays());
    }

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
    public ResponseEntity<Long> getAllHoursEmployeeBetweenDates(@PathVariable Integer tenantId,
                                                                @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                                @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
                                                                @RequestParam Integer idUser) {
        DataTimeContext<Long> context = DataTimeContext.<Long>builder().idUser(idUser).startDate(startDate)
                .tenantId(tenantId)
                .endDate(endDate).build();
        getHoursBetweenService.execute(context);
        return ResponseEntity.ok(context.getTotalHours());
    }

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
    public ResponseEntity<Long> getOvertimeEmployeeBetweenDates(@PathVariable Integer tenantId,
                                                                @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
                                                                @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
                                                                @RequestParam Integer idUser) {
        DataTimeContext<Long> context = DataTimeContext.<Long>builder().idUser(idUser).startDate(startDate)
                .endDate(endDate).tenantId(tenantId).build();
        getOvertimeBetweenService.execute(context);
        return ResponseEntity.ok(context.getTotalOvertimeHours());
    }

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
    @Override
    public ResponseEntity<Void> addLeaveDays(@PathVariable Integer tenantId, @PathVariable Integer idUser,
                                             @RequestBody LeaveReportRequestDto leaveReport) {
        DataTimeContext<LeaveReportRequestDto> context = DataTimeContext.<LeaveReportRequestDto>builder().idUser(idUser)
                .task(leaveReport)
                .tenantId(tenantId).build();
        addLeaveDaysService.execute(context);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
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

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
    @Override
    public ResponseEntity<List<MonthlyUserStatisticInfoDto>> getMonthStatistic(@PathVariable Integer tenantId,
                                                                               @RequestParam YearMonth targetMonth,
                                                                               @RequestParam Integer idUser,
                                                                               @RequestParam Boolean report) {
        DataTimeContext<MonthlyUserStatisticInfoDto> context = DataTimeContext.<MonthlyUserStatisticInfoDto>builder()
                .idUser(idUser)
                .idUser(idUser)
                .tenantId(tenantId).build();
        context.getStatisticInfoHolder().setReport(report);
        context.getStatisticInfoHolder().setStartOfMonth(targetMonth.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        statisticInfoService.execute(context);
        return ResponseEntity.ok(context.getStatisticInfoHolder().getMultipleUserData().values().stream().toList());
    }

    @PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
    public ResponseEntity<DataTimeDto> removeRecord(@PathVariable Integer tenantId, @PathVariable Integer id) {
        DataTimeContext<DataTimeDto> context =
                DataTimeContext.<DataTimeDto>builder().recordId(id).tenantId(tenantId).build();
        removeRecordService.execute(context);
        return ResponseEntity.ok(context.getSingleResponseDataTimeDto());
    }


}