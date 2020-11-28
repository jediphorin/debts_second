package com.noname.springsecurity.demo.service;

import com.noname.springsecurity.demo.crm.CrmDebt;
import com.noname.springsecurity.demo.crm.CrmDebtUpdate;
import com.noname.springsecurity.demo.crm.CrmUser;
import com.noname.springsecurity.demo.dao.DebtRepository;
import com.noname.springsecurity.demo.entity.Debt;
import com.noname.springsecurity.demo.entity.User;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {

	public Debt findById(int theId);
	
	public User findByUsername(String username);

	public void save(CrmUser crmUser);
	
	public void createTheDebt(CrmDebt crmDebt);
	
	public void updateTheDebt(CrmDebtUpdate crmDebtUpdate, int id, String username);
	
	public String findByNames(String first, String last, boolean userExists);
	
	public boolean userExists(String first, String last);

	//	метод админа
	public List<Debt> findAllDebts(String first, String last);
	
	public List<Debt> findAllDebtsToMeByUsername();
	
	public List<Debt> findMyDebtsToPeopleByUsername();
	
	public String getLogin();
	public void setLogin(String login);
	
	public int getId();
	public void setId(int id);

	public DebtRepository getDebtRepository();
	
	public void deleteById(int theId);
	
	public void undeleteById(int theId);

	public List<Debt> findDebtsForConfirmation();

	public void confirmUpdateTheDebt(int id);

	public void unconfirmUpdateTheDebt(int theDebtIdForUnconfirmation);

	public void toDelete(Debt theDebt, String username);

	public String selectExecutorUsername(Debt theDebt, String theUser);

	public List<Debt> findDebtsForDelete();
}
