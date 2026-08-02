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
import java.util.List;

import att.api.TimeTrackingApi;
import att.context.DataTimeContext;
import att.dto.DataTimeDto;
import att.dto.EditDataTimeUserDto;
import att.service.strategy.AddRecordEndService;
import att.service.strategy.AddRecordStartService;
import att.service.strategy.CheckNullRowsService;
import att.service.strategy.CountWorkedDaysService;
import att.service.strategy.EditRecordService;
import att.service.strategy.GetHoursBetweenService;
import att.service.strategy.GetOvertimeBetweenService;
import att.service.strategy.GetRecordsByDayService;
import att.service.strategy.GetRecordsByMonthService;
import att.service.strategy.RemoveRecordService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AttendanceTimeTrackingController implements TimeTrackingApi {

	private final AddRecordStartService addRecordStartService;
	private final AddRecordEndService addRecordEndService;
	private final EditRecordService editRecordService;
	private final GetRecordsByDayService getRecordsByDayService;
	private final CountWorkedDaysService countWorkedDaysService;
	private final GetRecordsByMonthService getRecordsByMonthService;
	private final RemoveRecordService removeRecordService;
	private final GetHoursBetweenService getHoursBetweenService;
	private final GetOvertimeBetweenService getOvertimeBetweenService;
	private final CheckNullRowsService checkNullRowsService;

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name()) || #idUser.toString() == authentication.name")
	public ResponseEntity<DataTimeDto> addRecordStart(@PathVariable Integer idUser) {
		DataTimeContext<DataTimeDto> context = DataTimeContext.<DataTimeDto>builder()
				.idUser(idUser).build();
		addRecordStartService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name()) || #idUser.toString() == authentication.name")
	public ResponseEntity<DataTimeDto> addRecordEnd(@PathVariable Integer idUser) {
		DataTimeContext<DataTimeDto> context = DataTimeContext.<DataTimeDto>builder()
				.idUser(idUser).build();
		addRecordEndService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
	public ResponseEntity<DataTimeDto> editRecord(@RequestBody EditDataTimeUserDto dataTimeDto) {
		DataTimeContext<DataTimeDto> context = DataTimeContext.<DataTimeDto>builder()
				.editDto(dataTimeDto).build();
		editRecordService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
	public ResponseEntity<List<DataTimeDto>> getAllRecordsByDay(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate localDate) {
		DataTimeContext<List<DataTimeDto>> context = DataTimeContext.<List<DataTimeDto>>builder()
				.date(localDate).build();
		getRecordsByDayService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
	public ResponseEntity<List<DataTimeDto>> getAllRecordsEmployeeByMonth(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
	                                                                      @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
	                                                                      @RequestParam Integer idUser) {
		DataTimeContext<List<DataTimeDto>> context = DataTimeContext.<List<DataTimeDto>>builder()
				.idUser(idUser).startDate(startDate).finishDate(finishDate).build();
		getRecordsByMonthService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
	public ResponseEntity<Long> getCountWorkedDaysByEmployee(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
	                                                         @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
	                                                         @RequestParam Integer idUser) {
		DataTimeContext<Long> context = DataTimeContext.<Long>builder()
				.idUser(idUser).startDate(startDate).finishDate(finishDate).build();
		countWorkedDaysService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
	public ResponseEntity<Long> getAllHoursEmployeeBetweenDates(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
	                                                            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
	                                                            @RequestParam Integer idUser) {
		DataTimeContext<Long> context = DataTimeContext.<Long>builder()
				.idUser(idUser).startDate(startDate).finishDate(finishDate).build();
		getHoursBetweenService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
	public ResponseEntity<Long> getOvertimeEmployeeBetweenDates(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
	                                                            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
	                                                            @RequestParam Integer idUser) {
		DataTimeContext<Long> context = DataTimeContext.<Long>builder()
				.idUser(idUser).startDate(startDate).finishDate(finishDate).build();
		getOvertimeBetweenService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
	public ResponseEntity<List<DataTimeDto>> checkRowsForNull(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
	                                                          @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
	                                                          @RequestParam Integer idUser) {
		DataTimeContext<List<DataTimeDto>> context = DataTimeContext.<List<DataTimeDto>>builder()
				.idUser(idUser).startDate(startDate).finishDate(finishDate).build();
		checkNullRowsService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}

	@PreAuthorize("hasRole(T(att.security.SecurityConstants.SecurityRoles).ADMINISTRATOR.name())")
	public ResponseEntity<DataTimeDto> removeRecord(@PathVariable Integer id) {
		DataTimeContext<DataTimeDto> context = DataTimeContext.<DataTimeDto>builder()
				.recordId(id).build();
		removeRecordService.execute(context);
		return ResponseEntity.ok(context.getResult());
	}
}