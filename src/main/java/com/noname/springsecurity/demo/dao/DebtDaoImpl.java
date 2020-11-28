package com.noname.springsecurity.demo.dao;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.noname.springsecurity.demo.entity.Debt;

@Repository
public class DebtDaoImpl implements DebtDao {
	
	@Autowired
	private EntityManager entityManager;
	
		//	долги мне
	@Override
	public List<Debt> findAllDebtsToMeByUsername(String username) {
		
		Session currentSession = entityManager.unwrap(Session.class);
		
		Query<Debt> theQuery = currentSession.createQuery("from Debt where creditorUsername=:username", Debt.class);
		theQuery.setParameter("username", username);
		
		List<Debt> theDebts = null;
		try {
			theDebts = theQuery.getResultList();
		} catch (Exception e) {
			theDebts = null;
		}
		
		return theDebts;
	}
	
	//	мои долги другим
	@Override
	public List<Debt> findMyDebtsToPeopleByUsername(String username) {
		
		Session currentSession = entityManager.unwrap(Session.class);
		
		Query<Debt> theQuery = currentSession.createQuery("from Debt where debtorUsername=:username", Debt.class);
		theQuery.setParameter("username", username);
		
		System.out.println("un " + username);
		
		List<Debt> theDebts = null;
		try {
			theDebts = theQuery.getResultList();
		} catch (Exception e) {
			theDebts = null;
			e.printStackTrace();
		}
		
		return theDebts;
	}
	
//	список долгов на удаление (executor_username и for_delete)
	@Override
	public List<Debt> findDebtsForDelete(String username) {
		
		Session currentSession = entityManager.unwrap(Session.class);
		
		Query<Debt> theQuery = currentSession.createQuery("from Debt where deleter=:username", Debt.class);
		theQuery.setParameter("username", username);

		System.out.println("я здесь");
		List<Debt> theDebts = null;
		
		try {
			theDebts = theQuery.getResultList();
			System.out.println("пустой ли список долгов на удаление? " + theDebts.isEmpty());
		} catch (Exception e) {
			theDebts = null;
		}
		
		return theDebts;
	}

	//	список долгов на обновление (executor_username и new_value)	РАБОТАЕТ!
	@Override
	public List<Debt> findDebtsForConfirmation(String username) {

		Session currentSession = entityManager.unwrap(Session.class);
		
		Query<Debt> theQuery = currentSession.createQuery("from Debt where executorUsername=:username and newValue is not NULL", Debt.class);
		theQuery.setParameter("username", username);
		
		System.out.println("я тут");
		List<Debt> theDebts = null;
		
		try {
			theDebts = theQuery.getResultList();
		} catch (Exception e) {
			theDebts = null;
		}
		
		return theDebts;
	}

	@Override
	public List<Debt> findDebtsByNames(String first, String last) {
		
		// get the current hibernate session
		Session currentSession = entityManager.unwrap(Session.class);
					
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		Query<Debt> theQuery = currentSession.createQuery("from User where firstName=:first and lastName=:last", Debt.class);
		theQuery.setParameter("first", first);
		theQuery.setParameter("last", last);
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
		List<Debt> theDebts = null;
		
		try {
			theDebts = theQuery.getResultList();
		} catch (Exception e) {
			theDebts = null;
		}
		
		return theDebts;
	}

	@Override
	public void save(Debt theDebt) {
		
		// get current hibernate session
		Session currentSession = entityManager.unwrap(Session.class);
		
		// create the debt ...
		currentSession.saveOrUpdate(theDebt);

	}
}
