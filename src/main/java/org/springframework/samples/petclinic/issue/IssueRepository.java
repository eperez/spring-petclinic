/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.issue;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository class for <code>Issue</code> domain objects All method names are compliant with Spring Data naming
 * conventions so this interface can easily be extended for Spring Data See here: http://static.springsource.org/spring-data/jpa/docs/current/reference/html/jpa.repositories.html#jpa.query-methods.query-creation
 *
 */
public interface IssueRepository extends Repository<Issue, Integer> {

    /**
     * Retrieve an {@link Issue} from the data store by id.
     * @param id the id to search for
     * @return the {@link Issue} if found
     */
    @Query("SELECT issue FROM Issue issue WHERE issue.id =:id")
    @Transactional(readOnly = true)
    Issue findById(@Param("id") Integer id);

    Collection<Issue> findAll();

    /**
     * Save an {@link Issue} to the data store, either inserting or updating it.
     * @param issue the {@link Issue} to save
     */
    void save(Issue issue);


}
