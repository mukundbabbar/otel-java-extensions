package com.mulabs.java;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Random;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/WebFrontEnd")
public class WebFrontEndController extends BaseController {

	public static String classVar = "web";
	
    @RequestMapping(value = {"","/"})
    public String home() {
    	System.out.println("WebFrontEnd:home()");
        return "Hello from WebFrontEnd:home()";
    }

	@RequestMapping("/login")
	public String login(HttpServletRequest request) {
    	System.out.println("WebFrontEnd:login()");
    	Random r = new Random();
    	int low = 10;
    	int high = 100;
    	int a = r.nextInt(high-low) + low;
    	int b = r.nextInt(high-low) + low;
    	topup("recharge","web-partner", "shop", high);
    	Name c = new Name();
    	sum(a,b);
    	minus(a,b);
    	String aa = "Customer_A";
    	pay(aa,a);
    	findName(c, a);
		return "Hello from login";
	}
	
	private String getName()
	{
		return "Retail";
	}
	private String pay(String aa, int b)
	{
		System.out.println("paying");
		return "paying";
	}

	private int findName(Name c, int a) {
		return c.getSquare(a);
	}	
	private int sum(int a, int b) {
		return a+b;
		
	}
	
	private int minus(int a, int b) {
		return a-b;
		
	}
	
	private int divide(int a, int b) {
		return a/b;
		
	}

	private int topup(String prodType, String partner, String brand, int amount) {
		//return dummy status code
		return 200;
	}

	@RequestMapping("/pgp")
	public String pgp(HttpServletRequest request) {
		return "Hello from purchaseGamePass";
	}

	@RequestMapping("/jg")
	public String jg(HttpServletRequest request) {
		makeWebRequest("acct-mgmt", "8080", "AcctMgmt/jg", request);
		return "Hello from jg";
	}


}