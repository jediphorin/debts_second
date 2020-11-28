package com.noname.springsecurity.demo.service;

import com.noname.springsecurity.demo.crm.CrmDebt;
import com.noname.springsecurity.demo.crm.CrmDebtUpdate;
import com.noname.springsecurity.demo.crm.CrmUser;
import com.noname.springsecurity.demo.dao.DebtDao;
import com.noname.springsecurity.demo.dao.DebtRepository;
import com.noname.springsecurity.demo.dao.RoleDao;
import com.noname.springsecurity.demo.dao.UserDao;
import com.noname.springsecurity.demo.entity.Debt;
import com.noname.springsecurity.demo.entity.Role;
import com.noname.springsecurity.demo.entity.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
	
	@Autowired
	private DebtRepository debtRepository;
	private String login;
	private int id;
	//	private int idForControl;

	// need to inject user dao
	@Autowired
	private UserDao userDao;

	@Autowired
	private RoleDao roleDao;
	
	@Autowired
	private DebtDao debtDao;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	//	перенос долга в список на удаление
	@Override
	@Transactional
	public void toDelete(Debt theDebt, String username) {
				
		theDebt.setDeleter(this.selectExecutorUsername(theDebt, username));		
		
		System.out.println("\ndolg: \n" + theDebt.toString());	//
		debtDao.save(theDebt);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	//	отмена удаления долга
	@Override
	@Transactional
	public void undeleteById(int theId) {
		
		Debt theDebt = this.findById(theId);
		
		theDebt.setDeleter(null);

		debtDao.save(theDebt);
	}
	
	//	удаление долга по айдишнику
	@Override
	@Transactional
	public void deleteById(int theId) {
		debtRepository.deleteById(theId);
	}

	//	поиск конкретного долга по айдишнику
	@Override
	@Transactional
	public Debt findById(int theId) {
		
		System.out.println("HA4AJIO");

		Optional<Debt> result = debtRepository.findById(theId);
		
		System.out.println("PRODOLZHENIE");
		
		Debt theDebt = null;
		
		if (result.isPresent()) {
			theDebt = result.get();
			System.out.println("DEBT: " + theDebt);
		}
				
		else 
				throw new RuntimeException("Did not find debt id - " + theId);
		System.out.println("KOHELI");
		return theDebt;
	}
	
	//	список долгов для удаления
	@Override
	@Transactional
	public List<Debt> findDebtsForDelete() {
		return debtDao.findDebtsForDelete(getLogin());
	}

	//	список долгов для обновления
	@Override
	@Transactional
	public List<Debt> findDebtsForConfirmation() {
		return debtDao.findDebtsForConfirmation(getLogin());
	}

	//	список моих должников
	@Override
	@Transactional
	public List<Debt> findAllDebtsToMeByUsername() {
		return debtDao.findAllDebtsToMeByUsername(getLogin());
	}
	
	//	список моих долгов другим
	@Override
	@Transactional
	public List<Debt> findMyDebtsToPeopleByUsername() {
		return debtDao.findMyDebtsToPeopleByUsername(getLogin());
	}	

	//	утверждение изменения долга (2-ой этап)
	@Transactional
	@Override
	public void confirmUpdateTheDebt(int id) {
		
		Debt theDebt = this.findById(id);
		theDebt.setValue(theDebt.getNewValue());
		
		theDebt.setExecutorUsername(null);
		theDebt.setNewValue(null);
		
		System.out.println("UserServiceImpl: ПОДТВЕРЖДЕНИЕ ОБНОВЛЕНИЯ ДОЛГА");
		
		debtDao.save(theDebt);
	}
	
	//	отмена изменения долга (2-ой этап)
	@Transactional
	@Override
	public void unconfirmUpdateTheDebt(int theDebtIdForUnconfirmation) {

		Debt theDebt = this.findById(theDebtIdForUnconfirmation);
				
		theDebt.setExecutorUsername(null);
		theDebt.setNewValue(null);
		
		System.out.println("UserServiceImpl: НЕУТВЕРЖДЕНИЕ ОБНОВЛЕНИЯ ДОЛГА");
		
		debtDao.save(theDebt);
	}

	//	предлагает изменение (1-ый этап)
	@Override
	@Transactional
	public void updateTheDebt(CrmDebtUpdate crmDebtUpdate, int id, String username) {
		
		Debt theDebt = this.findById(id);
		
		System.out.println("кредитор " + theDebt.getCreditorUsername() + ", должник " + theDebt.getDebtorUsername());
		
		theDebt.setNewValue(crmDebtUpdate.getValue());

		String executorUsername = this.selectExecutorUsername(theDebt, username);
		theDebt.setExecutorUsername(executorUsername);

		System.out.println(executorUsername);
		executorUsername = null;
		
		System.out.println("UserServiceImpl: ОБНОВЛЕНИЕ ДОЛГА");
		debtDao.save(theDebt);
	}
	
	//	вспомогательный метод выбора юзера-исполнителя (удалений и обновлений данных)
	@Override
	public String selectExecutorUsername(Debt theDebt, String theUser) {
		
		ArrayList<String> usernames = new ArrayList<>();;
		usernames.add(theDebt.getCreditorUsername());
		usernames.add(theDebt.getDebtorUsername());
		
		String executorUsername="";
		
		for (int i = 0; i < usernames.size(); i++) {
			if (!(theUser.equals(usernames.get(i))))
				executorUsername = usernames.get(i);
		}
		
		return executorUsername;
	}
	
	//	создание долга
	@Override
	@Transactional
	public void createTheDebt(CrmDebt crmDebt) {
		
		//	обнуляющийся флаг существования участника долга в системе
		boolean debtorOrCreditorExists = false;
		
		//	необнуляющийся флаг существования хотя бы одного участника долга в системе
		boolean existsForFinal = false;

		Debt debt = new Debt();
		
		String debtorFirstName = crmDebt.getDebtorFirstName();
		String debtorLastName = crmDebt.getDebtorLastName();
		
		debt.setDebtorFirstName(debtorFirstName);
		debt.setDebtorLastName(debtorLastName);
		
		System.out.println("имя должника: " + debt.getDebtorFirstName());
		System.out.println("фамилия должника: " + debt.getDebtorLastName());
		
		if (userExists(debtorFirstName, debtorLastName)) {
			debtorOrCreditorExists = true;
			existsForFinal = true;
		}
			
		String debtorUsername = findByNames(debt.getDebtorFirstName(), debt.getDebtorLastName(), debtorOrCreditorExists);
		debt.setDebtorUsername(debtorUsername);
		System.out.println("юзернейм должника: " + debt.getDebtorUsername());
		
		String creditorFirstName = crmDebt.getCreditorFirstName();
		String creditorLastName = crmDebt.getCreditorLastName();
		
		debt.setCreditorFirstName(creditorFirstName);
		debt.setCreditorLastName(creditorLastName);
		
		System.out.println("имя кредитора: " + debt.getCreditorFirstName());
		System.out.println("фамилия кредитора: " + debt.getCreditorLastName());
		
		debtorOrCreditorExists = false;
		
		if (userExists(creditorFirstName, creditorLastName)) {
			debtorOrCreditorExists = true;
			existsForFinal = true;
		}
		
		String creditorUsername = findByNames(debt.getCreditorFirstName(), debt.getCreditorLastName(), debtorOrCreditorExists);
		debt.setCreditorUsername(creditorUsername);
		System.out.println("юзернейм кредитора: " + debt.getCreditorUsername());
		
		debt.setInfo(crmDebt.getInfo());
		debt.setValue(crmDebt.getValue());
		
		System.out.println("описание долга: " + debt.getInfo());
		System.out.println("величина долга: " + debt.getValue());
		
		ArrayList<User> userList = new ArrayList<>();
		userList.add(findByUsername(debtorUsername));
		userList.add(findByUsername(creditorUsername));
		
		if (existsForFinal) {
			System.out.println("UserServiceImpl: идёт создание долга");
			debt.setExecutorUsername(null);
			debt.setNewValue(null);

			debtDao.save(debt);
		}
		else {
			System.out.println("ни одного из участников долга не зарегистрировано в системе!");
			debt = null;
		}
	}
	
	//	сохранение пользователя в БД при создании пользователя
	@Override
	@Transactional
	public void save(CrmUser crmUser) {
		User user = new User();
		
		// assign user details to the user object
		user.setUsername(crmUser.getUsername());
		user.setPassword(passwordEncoder.encode(crmUser.getPassword()));
		user.setFirstName(crmUser.getFirstName());
		user.setLastName(crmUser.getLastName());
		user.setEmail(crmUser.getEmail());

		// give user default role of "employee"
		user.setRoles(Arrays.asList(roleDao.findRoleByName("ROLE_EMPLOYEE")));

		 // save user in the database
		userDao.save(user);
	}
	
	//	проверка на существование юзернейма в системе
	@Override
	@Transactional
	public User findByUsername(String username) {
		// check the database if the user already exists
		return userDao.findByUsername(username);
	}
	
	//	проверка существования пользователя в системе
	@Override
	@Transactional
	public boolean userExists(String first, String last) {
		return userDao.userExists(first, last);
	}

	//	поиск юзернейма по имени и фамилии
	@Override
	@Transactional
	public String findByNames(String first, String last, boolean userExists) {
		return userDao.findByNames(first, last, userExists);
	}

	private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
		return roles.stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userDao.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("Invalid username or password.");
		}
		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				mapRolesToAuthorities(user.getRoles()));
	}
	
	//	метод админа
	@Override
	@Transactional
	public List<Debt> findAllDebts(String first, String last) {
		return debtDao.findDebtsByNames(first, last);
	}
	
	@Override
	public String getLogin() {return login;}
	@Override
	public void setLogin(String login) {this.login = login;}
	
	@Override
	public int getId() {return id;}
	@Override
	public void setId(int id) {this.id = id;}
	
	@Override
	public DebtRepository getDebtRepository() {return debtRepository;}
	
}
