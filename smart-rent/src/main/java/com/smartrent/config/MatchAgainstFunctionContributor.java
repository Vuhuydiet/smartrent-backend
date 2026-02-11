package com.smartrent.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Registers MySQL FULLTEXT MATCH...AGAINST as a Hibernate function
 * so it can be used in JPA Criteria queries via criteriaBuilder.function("match_against", ...)
 */
public class MatchAgainstFunctionContributor implements FunctionContributor {
    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
                "match_against",
                "MATCH(?1) AGAINST(?2 IN BOOLEAN MODE)",
                functionContributions.getTypeConfiguration()
                        .getBasicTypeRegistry()
                        .resolve(StandardBasicTypes.DOUBLE)
        );
    }
}
