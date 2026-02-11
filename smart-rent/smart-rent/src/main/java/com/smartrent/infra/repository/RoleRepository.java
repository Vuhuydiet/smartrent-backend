package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
  List<Role> getRolesByRoleIdIn(Collection<String> roleId);
}
