package main.repository;

import main.model.GlobalVariable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalVariablesRepository extends JpaRepository<GlobalVariable, Integer> {
}
