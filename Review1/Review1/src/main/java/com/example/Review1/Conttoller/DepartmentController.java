package com.example.Review1.Conttoller;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Review1.Entity.DepartmentEntity;
import com.example.Review1.Service.DepartmentService;

import ch.qos.logback.classic.Logger;

@RestController
@RequestMapping("/Department")
public class DepartmentController {
	
	@Autowired
	DepartmentService dep_serv;
	
	private static Logger lg=(Logger) LoggerFactory.getLogger(DepartmentController.class);
	
	@PostMapping("/add")
	public void addDepartment(@RequestBody DepartmentEntity dep) {
		lg.warn("The request is in the adddep in the dep controller");
		dep_serv.addDep(dep);
		lg.warn("The request has exited from the adddep in the dep controller");
	}
	
	@GetMapping("/get")
	public List<DepartmentEntity> getDepartment(){
		lg.warn("The request is in the getdep in the dep controller");
		return dep_serv.getall();
	}
	
}
