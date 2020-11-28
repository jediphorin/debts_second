package com.noname.springsecurity.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.noname.springsecurity.demo.crm.CrmDebt;
import com.noname.springsecurity.demo.crm.CrmDebtUpdate;
import com.noname.springsecurity.demo.entity.Debt;
import com.noname.springsecurity.demo.service.UserService;

@Controller
@RequestMapping("/debt")
public class DebtController {
	
	@Autowired
    private UserService userService;
	private ArrayList<Integer> idControl;
	private Logger logger = Logger.getLogger(getClass().getName());

	@InitBinder
	public void initBinder(WebDataBinder dataBinder) {
		StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(true);
		dataBinder.registerCustomEditor(String.class, stringTrimmerEditor);
	}
	
	//	предложения к удалению долга (1-ый этап) (БРАК)
	@GetMapping("/toDelete")
	public String toDelete (@RequestParam("debtId") int theId) {
		
		String username = userService.getLogin();	//
		
		Debt theDebt = userService.findById(theId);
		
		try {
			userService.toDelete(theDebt, username);		//
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return "/home";
	}
	
	//	УДАЛЕНИЕ конкретное (2-ой этап)
	@GetMapping("/delete")
	public String delete(@RequestParam("debtId") int theId) {
		
		userService.deleteById(theId);
		
		return "/home";
	}
	
	//	отмена удаления (2-ой этап)
	@GetMapping("/undelete")
	public String undelete(@RequestParam("debtId") int theId) {
		
		userService.undeleteById(theId);
		
		return "/home";
	}
	
	//	форма для внесения изменений в долг (1-ый этап)
	@GetMapping("/showDebtFormForUpdate")
	public String showDebtFormForUpdate(@RequestParam("debtId") int theId, Model theModel) {
		
		System.out.println("DEBT CONTROLLER!");

		//	нахожу долг в БД по theId
		Debt theDebt = userService.findById(theId);
		
		//	set debt as a model attribute to pre-popelate the form
		theModel.addAttribute("debt", theDebt);
		
		CrmDebtUpdate crmDebtUpdate = new CrmDebtUpdate();
		
		// НАДО ЛИ ПРИСВАИВАТЬ NULL ПОСЛЕ ИСПОЛНЕНИЯ?
		userService.setId(theDebt.getIdOfDebt());
		
		if (idControl == null)
			idControl = new ArrayList<>();
			
		idControl.add(theDebt.getIdOfDebt());
		System.out.println("SHOW: ArrayList size = " + idControl.size() + ", debt id = " + theDebt.getIdOfDebt());
		
		if (idControl.size() > 1) {
		
			if (idControl.get(0) != idControl.get(1) || idControl.size() > 2) {
				
				System.out.println("SHOW: контроль айди провален!");
				
				idControl = null;
				
				return "/home";
			}
		}
		
		System.out.println("ID = " + userService.getId());
		crmDebtUpdate.setValue(theDebt.getValue());
		System.out.println("showDebtFormForUpdate(): crmDebtUpdate value — " + crmDebtUpdate.getValue());
		theModel.addAttribute("crmDebtUpdate", crmDebtUpdate);
		
		return "debts/debt-update-form";
	}
	
	//	утверждение изменение долга (2-ой этап)
	@GetMapping("/processConfirmationForm")
	public String processConfirmationForm(@RequestParam("theDebtIdForConfirmation") int theDebtIdForConfirmation) {
		
		logger.info("1_Processing debt CONFIRMATION update form by: " + userService.getLogin());
		
		userService.confirmUpdateTheDebt(theDebtIdForConfirmation);
		
		return "debts/debt-registration-confirmation";
	}
	//	отмена изменения долга (2-ой этап)
	@GetMapping("/unProcessConfirmationForm")
	public String unProcessConfirmationForm(@RequestParam("theDebtIdForUnconfirmation") int theDebtIdForUnconfirmation) {
		
		logger.info("1_Processing debt UNconfirmation update form by: " + userService.getLogin());
		
		userService.unconfirmUpdateTheDebt(theDebtIdForUnconfirmation);
		
		return "debts/debt-registration-confirmation";
	}
	
	//	форма создания долга
	@GetMapping("/showDebtRegistrationForm")
	public String showDebtRegistrationForm(Model theModel) {
		
		theModel.addAttribute("crmDebt", new CrmDebt());
		
		return "debts/debt-registration-form";
	}
	
	//	список долгов на изменение
	@GetMapping("/showMyConfirmations")
	public String showMyConfirmations(Model theModel) {
		
		List<Debt> debtsForConfirmation = userService.findDebtsForConfirmation();
		theModel.addAttribute("confirmations", debtsForConfirmation);
		
		return "/debts/debt-update-confirmation-form.html";
	}
	
	//	список долгов на удаление РАБОТАЕТ
	@GetMapping("/showDelete")
	public String showDelete (Model theModel) {
		
		List<Debt> debtsForDelete = userService.findDebtsForDelete();
		theModel.addAttribute("listForDelete", debtsForDelete);
		
		System.out.println("controller delete");
		
		return "/debts/debt-delete-confirmation.html";
	}
	
	//	список долгов
	@GetMapping("/showMyDebts")
	public String showMyDebts(Model theModel) {
		
		List<Debt> myDebts = userService.findMyDebtsToPeopleByUsername();
		theModel.addAttribute("myDebts", myDebts);
		
		List<Debt> debts = userService.findAllDebtsToMeByUsername();
		theModel.addAttribute("myDebtors", debts);
		
		return "debts/my-debt";
	}
	
//	создание долга
	@PostMapping("/processDebtRegistrationForm")
	public String processDebtRegistrationForm(
				@Valid @ModelAttribute("crmDebt") CrmDebt theCrmDebt, 
				BindingResult theBindingResult, 
				Model theModel) {
		
		logger.info("Processing debt registration form: debtor: " + theCrmDebt.getDebtorFirstName() + " " + theCrmDebt.getDebtorLastName() 
		+ "\n, creditor: " + theCrmDebt.getCreditorFirstName() + " " + theCrmDebt.getCreditorLastName());
		
		// form validation
		 if (theBindingResult.hasErrors()){
			 return "debts/debt-registration-form";
	        }
        
		 System.out.println("quest!!!");
		 
        // create debt        						
        userService.createTheDebt(theCrmDebt);
        
        logger.info("Successfully a debt: " + theCrmDebt.toString());
   
        return "debts/debt-registration-confirmation";	
	}
	
	//	исполнение формы для изменения долга
	@PostMapping("/processDebtUpdateForm")
	public String processDebtUpdateForm(
			@Valid @ModelAttribute("crmDebtUpdate") CrmDebtUpdate theCrmDebtUpdate,
				BindingResult theBindingResult, 
				Model theModel) {
		
		logger.info("Processing debt update form: debtor — " + theCrmDebtUpdate.getValue());
		
		idControl.add(userService.getId());
		
		System.out.println("PROCESS: ArrayList size = " + idControl.size()  + ", debt id = " + idControl.get(1));
		
		for (int i = 0; i < idControl.size(); i++) 
			System.out.println("cycle. Id: " + idControl.get(i));
		
		
		if (idControl.get(0) != idControl.get(1) || idControl.size() > 2) {
			
			System.out.println("PROCESS: контроль айди провален!");
			
			idControl = null;
			
			return "/home";
		}
		
		// form validation
		if (theBindingResult.hasErrors())
			 return "debts/debt-registration-form";
	        
		System.out.println("processDebtUpdateForm(): crmDebtUpdate value — " + theCrmDebtUpdate.getValue());
		  	
		//	ВАЖНО!!!
        userService.updateTheDebt(theCrmDebtUpdate, userService.getId(), userService.getLogin());
        
        idControl = null;
        
        return "debts/debt-registration-confirmation";		
	}
	
}
