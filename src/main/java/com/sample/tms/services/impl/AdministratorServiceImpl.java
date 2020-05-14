package com.sample.tms.services.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sample.tms.entity.Administrator;
import com.sample.tms.repository.AdministratorRepository;
import com.sample.tms.services.AdministratorService;

@Service
public class AdministratorServiceImpl implements AdministratorService {

	@Autowired
	private AdministratorRepository administratorRepository;
	
	

	@Override
	public Iterable<Administrator> listAdmin() {

		return administratorRepository.findAll();

	}

	@Override
	public Optional<Administrator> findOne(Long id) {
		return administratorRepository.findById(id);
	}

	@Override
	public Administrator addAdmin(Administrator administrator) {
		return administratorRepository.save(administrator);
	}

	@Override
	public String deleteAdmin(Long id) {
		administratorRepository.deleteById(id);
		return "{'Message':'Admin Deleted Succefully'}";
	}

}
