package co.il.avivsmile.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import co.il.avivsmile.dto.DataTimeDto;
import co.il.avivsmile.dto.EditDataTimeUserDto;
import co.il.avivsmile.service.DataTimeService;

@RequestMapping("/record")
@RestController
public class DataTimeController {
	
	@Autowired
	DataTimeService service;
	
	
	@PutMapping("/start/{idUser}")
	public DataTimeDto addRecordStart(@PathVariable Integer idUser) {
		return service.addRecordStart(idUser);
	}
	
	@PutMapping("/finish/{idUser}")
	public DataTimeDto addRecordEnd(@PathVariable Integer idUser) {
		return service.addRecordEnd(idUser);
	}
	
	
	@PostMapping()
	public DataTimeDto editRecord(@RequestBody EditDataTimeUserDto dataTimeDto) {
		return service.editRecord(dataTimeDto);
	}
	
	@GetMapping("/records")
	public List<DataTimeDto>getAllRecordsByDay(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate localDate){
		return service.getAllRecordsByDay(localDate);
	}
	
	@GetMapping("/range/records")
	public List<DataTimeDto>getAllRecordsEmployeeByMoth(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
														@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
														@RequestParam Integer idUser){
		return service.getAllRecordsEmployeeByMonth(idUser, startDate, finishDate);
	}
	
	
	
	@GetMapping("/workdays")
	public Long getCountWorkedDaysByEmployee(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
			@RequestParam Integer idUser){
		return service.getCountWorkedDaysByEmployee(idUser, startDate, finishDate);
	}
	
	@GetMapping("/hours")
	public Long getAllHoursEmployeeBetweenDates(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
			@RequestParam Integer idUser){
		return service.getAllHoursEmployeeBetweenDates(idUser, startDate, finishDate);
		}
	
	@GetMapping("/overtime")
	public Long getOvertimeEmployeeBetweenDates(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
			@RequestParam Integer idUser){
		return service.getOvertimeEmployeeBetweenDates(idUser, startDate, finishDate);
	}
	
	@GetMapping("/check")
	public List<DataTimeDto> checkRowsForNull(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
								@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate finishDate,
								@RequestParam Integer idUser){
		return service.checkRowsForNull(idUser, startDate, finishDate);
	}
	
	@DeleteMapping("/remove/{id}")
	public DataTimeDto removeRecord(@PathVariable Integer id) {
		return service.removeRecord(id);
	}

}
