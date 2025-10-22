package vn.hoangtung.jobhunter.repository;

import org.springframework.stereotype.Repository;

import vn.hoangtung.jobhunter.domain.Company;
import vn.hoangtung.jobhunter.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

}
