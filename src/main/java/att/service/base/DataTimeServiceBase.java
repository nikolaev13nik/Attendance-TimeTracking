package att.service.base;

import org.springframework.beans.factory.annotation.Autowired;

import att.context.DataTimeContext;
import att.dao.UserRepository;
import att.dao.UserTimeRepository;
import att.exceptions.RecordNotFoundException;
import att.exceptions.UserNotFoundException;
import att.mapper.DataTimeMapper;
import att.model.DataTime;
import att.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

public abstract class DataTimeServiceBase<R> implements BaseService<R> {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserTimeRepository timeRepository;

    @Autowired
    protected DataTimeMapper mapper;

    @PersistenceContext
    protected EntityManager em;

    @Transactional
    @Override
    public void execute(DataTimeContext<R> context) {
        executeWithoutTransactional(context);
    }

    @Override
    public void executeWithoutTransactional(DataTimeContext<R> context) {
        fetchAndValidate(context);
        executeBusiness(context);
        persist(context);
        mapResult(context);
    }

    protected void fetchAndValidate(DataTimeContext<R> context) {
        context.setUser(findUserOrThrow(context.getIdUser()));
    }

    protected void executeBusiness(DataTimeContext<R> context) {
    }

    protected void persist(DataTimeContext<R> context) {
    }

    protected void mapResult(DataTimeContext<R> context) {
    }

    protected User findUserOrThrow(Integer idUser) {
        return userRepository.findById(idUser)
                .orElseThrow(() -> new UserNotFoundException(idUser.toString()));
    }

    protected DataTime findRecordOrThrow(Integer id) {
        return timeRepository.findById(id).orElseThrow(RecordNotFoundException::new);
    }
}