package co.il.avivsmile.service;

import java.time.LocalDate;
import java.util.List;

import co.il.avivsmile.dto.DataTimeDto;
import co.il.avivsmile.dto.EditDataTimeUserDto;


public interface DataTimeService {
	
	DataTimeDto addRecordStart(Integer idUser);
	DataTimeDto addRecordEnd(Integer idUser);
	
	
	
	DataTimeDto editRecord(EditDataTimeUserDto dataTimeDto);
	List<DataTimeDto>getAllRecordsByDay(LocalDate localDate);
	
	List<DataTimeDto>getAllRecordsEmployeeByMonth(Integer idUser, LocalDate localDateStart, LocalDate localDateFinish);
	
	
	
	Long getCountWorkedDaysByEmployee(Integer idUser, LocalDate localDateStart, LocalDate localDateFinish);
	
	Long getAllHoursEmployeeBetweenDates(Integer idUser, LocalDate localDateStart, LocalDate localDateFinish);
	
	Long getOvertimeEmployeeBetweenDates(Integer idUser, LocalDate localDateStart, LocalDate localDateFinish);
	
	
	List<DataTimeDto>checkRowsForNull(Integer idUser,LocalDate localDateStart, LocalDate localDateFinish);
	
	DataTimeDto removeRecord(Integer id);
	

}
