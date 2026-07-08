package co.il.avivsmile.service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.il.avivsmile.dao.UserRepository;
import co.il.avivsmile.dao.UserTimeRepository;
import co.il.avivsmile.dto.DataTimeDto;
import co.il.avivsmile.dto.EditDataTimeUserDto;
import co.il.avivsmile.dto.UserDto;
import co.il.avivsmile.exceptions.RecordNotFoundException;
import co.il.avivsmile.exceptions.UserNotFoundException;
import co.il.avivsmile.model.DataTime;
import co.il.avivsmile.model.User;

@Service
public class DataTimeServiceImpl implements DataTimeService {

	@Autowired
	UserTimeRepository timeRepository;
	
	@Autowired
	UserRepository accountRepository;
	
	@PersistenceContext
	EntityManager em;
	
//	@Autowired
//	private  SessionFactory sessionFactory;
	

	@Transactional
	@Override
	public DataTimeDto addRecordStart(Integer idUser) {
		User user = accountRepository.findById(idUser)
				.orElseThrow(() -> new UserNotFoundException(idUser.toString()));
		DataTime dataTime = timeRepository.save(new DataTime(user,LocalDate.now(),LocalDateTime.now(),null));
		return convertToDataTimeDto(dataTime,user);
	}

	private DataTimeDto convertToDataTimeDto(DataTime dataTime,User user) {
		return DataTimeDto.builder()
				.id(dataTime.getId())
				.user(convertUserToUserDto(user))
				.date(dataTime.getDate())
				.start(dataTime.getStart())
				.finish(dataTime.getFinish())
				.build();
	}

	private UserDto convertUserToUserDto(User user) {
		
		return UserDto.builder()
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.idUser(user.getIdUser())
				.build();
	}

	@Transactional
	@Override
	public DataTimeDto addRecordEnd(Integer idUser) {
		User user = accountRepository.findById(idUser)
				.orElseThrow(() -> new UserNotFoundException(idUser.toString()));
		DataTime dataTime = null;
		TypedQuery<DataTime> query=em.createQuery("select h from DataTime h where h.user.idUser=?1 and h.date=?2 ORDER BY id DESC",DataTime.class);
		query.setParameter(1, idUser);
		query.setParameter(2, LocalDate.now());
		List<DataTime> list= query.getResultList();
		
		if (list.size()!=0 && list.get(0).getFinish()==null) {
			dataTime=list.get(0);
			dataTime.setFinish(LocalDateTime.now());
		}else {
			dataTime=timeRepository.save(new DataTime(user, LocalDate.now(), null, LocalDateTime.now()));
		}		
		return convertToDataTimeDto(dataTime,user);
	}

	@Override
	public List<DataTimeDto> getAllRecordsByDay(LocalDate localDate) {
		TypedQuery<DataTime> query=em.createQuery("select h from DataTime h where h.date=?1 ORDER BY id",DataTime.class);
			query.setParameter(1, localDate);
			List<DataTime>list=query.getResultList();	
		return convertToListDataTimeDto(list);
	}

	private List<DataTimeDto> convertToListDataTimeDto(List<DataTime> list) {
		return	list.stream().map(model->convertToDataTimeDto(model,model.getUser())).collect(Collectors.toList()); 
	}
	
	@Override
	public List<DataTimeDto> getAllRecordsEmployeeByMonth(Integer idUser, LocalDate localDateStart,
			LocalDate localDateFinish) {
		accountRepository.findById(idUser)
				.orElseThrow(() -> new UserNotFoundException(idUser.toString()));
		TypedQuery<DataTime> query=em.createQuery("select h from DataTime h where h.user.idUser=?1 and date BETWEEN :start and :finish",DataTime.class);
		query.setParameter(1,idUser);
		query.setParameter("start", localDateStart);
		query.setParameter("finish", localDateFinish);
		List<DataTime>list=query.getResultList();	
	return convertToListDataTimeDto(list);
	}
	
	
	

	@Override
	public Long getCountWorkedDaysByEmployee(Integer idUser, LocalDate localDateStart,
			LocalDate localDateFinish) {
		accountRepository.findById(idUser)
				.orElseThrow(() -> new UserNotFoundException(idUser.toString()));
		TypedQuery<Long> query=em.createQuery("select COUNT(distinct date) from DataTime h where h.user.idUser=?1 and date BETWEEN :start and :finish",Long.class);
		query.setParameter(1,idUser);
		query.setParameter("start", localDateStart);
		query.setParameter("finish", localDateFinish);
		Long count=query.getResultList().get(0);	
		return count;
	}

	@Override
	public Long getAllHoursEmployeeBetweenDates(Integer idUser, LocalDate localDateStart,
			LocalDate localDateFinish) {
		 accountRepository.findById(idUser)
				.orElseThrow(() -> new UserNotFoundException(idUser.toString()));
		Query query=em.createQuery("select SUM(datediff(minute,h.start,h.finish)) from DataTime h where id_user=?1 and date BETWEEN :start and :finish");
				query.setParameter(1, idUser);
				query.setParameter("start", localDateStart);
				query.setParameter("finish", localDateFinish);		
		return (Long) query.getResultList().get(0);
	}

	@Override
	public Long getOvertimeEmployeeBetweenDates(Integer idUser, LocalDate localDateStart,
			LocalDate localDateFinish) {
		 accountRepository.findById(idUser)
				.orElseThrow(() -> new UserNotFoundException(idUser.toString()));
Query query=em.createQuery("select SUM(datediff(minute,h.start,h.finish)-480) from DataTime h where id_user=?1 and (date BETWEEN :start and :finish) and datediff(minute,h.start,h.finish)>480");
		query.setParameter(1, idUser);
		query.setParameter("start", localDateStart);
		query.setParameter("finish", localDateFinish);
		return (Long) query.getResultList().get(0);
	}

	
	@Transactional
	@Override
	public DataTimeDto editRecord(EditDataTimeUserDto editDataTimeUserDtoo) {
//		User user = accountRepository.findById(editDataTimeUserDtoo.getId())
//				.orElseThrow(() -> new UserNotFoundException(editDataTimeUserDtoo.getId().toString()));
		DataTime dataTime=timeRepository.findById(editDataTimeUserDtoo.getId()).orElseThrow(RecordNotFoundException::new);
		if(editDataTimeUserDtoo.getStart()!=null) {
			dataTime.setStart(editDataTimeUserDtoo.getStart());
		}
		if(editDataTimeUserDtoo.getFinish()!=null) {
			dataTime.setFinish(editDataTimeUserDtoo.getFinish());
		}	
		return convertToDataTimeDto(dataTime,dataTime.getUser());
	}

	@Override
	public List<DataTimeDto> checkRowsForNull(Integer idUser, LocalDate localDateStart, LocalDate localDateFinish) {
		accountRepository.findById(idUser).orElseThrow(() -> new UserNotFoundException(idUser.toString()));
		TypedQuery<DataTime> query=em.createQuery("select h from DataTime h where h.user.idUser=?1 and (date BETWEEN :start and :finish) and (h.start is null or h.finish is null)",DataTime.class);
	   	query.setParameter(1,idUser);
		 query.setParameter("start", localDateStart);
		 query.setParameter("finish", localDateFinish);
		List<DataTime>list=query.getResultList();	
	return convertToListDataTimeDto(list);
	}

	@Override
	public DataTimeDto removeRecord(Integer id) {

		DataTime dataTime=timeRepository.findById(id).orElseThrow(RecordNotFoundException::new);
		timeRepository.delete(dataTime);
		return convertToDataTimeDto(dataTime,dataTime.getUser());
	}

}
