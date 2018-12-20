/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Controller
class IssueController {

    private static final String VIEWS_ISSUE_CREATE_OR_UPDATE_FORM = "issues/createOrUpdateIssueForm";
    public static final String TABLE = "issues";

    private final IssueRepository issues;
    private final JdbcTemplate jdbcTemplate;

    public IssueController(IssueRepository clinicService, JdbcTemplate jdbcTemplate) {
        this.issues = clinicService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    @GetMapping("/issues/new")
    public String initCreationForm(Map<String, Object> model) {
        Issue issue = new Issue();
        model.put("issue", issue);
        return VIEWS_ISSUE_CREATE_OR_UPDATE_FORM;
    }

    @PostMapping("/issues/new")
    public String processCreationForm(@Valid Issue issue, BindingResult result) {
        if (result.hasErrors()) {
            return VIEWS_ISSUE_CREATE_OR_UPDATE_FORM;
        } else {
            this.issues.save(issue);
            return "redirect:/issues/" + issue.getId();
        }
    }

    @GetMapping("/issues")
    public String processFindForm(Map<String, Object> model) {
        Collection<Issue> results = this.issues.findAll();
        model.put("issues", results);
        return "issues/issuesList";
    }

    @GetMapping("/issues/{issueId}/edit")
    public String initUpdateIssueForm(@PathVariable("issueId") int issueId, Model model) {
        Issue issue = this.issues.findById(issueId);
        model.addAttribute(issue);
        return VIEWS_ISSUE_CREATE_OR_UPDATE_FORM;
    }

    @PostMapping("/issues/{issueId}/edit")
    public String processUpdateIssueForm(@Valid Issue issue, @RequestParam Map<String,String> allRequestParams, BindingResult result, @PathVariable("issueId") int issueId) throws Exception {
        if (result.hasErrors()) {
            return VIEWS_ISSUE_CREATE_OR_UPDATE_FORM;
        } else {
            final Map<String,OldNewValue> values = new HashMap<>();

            for (final Map.Entry<String,String> requestParam : allRequestParams.entrySet()) {
                final String paramName = requestParam.getKey();
                final String paramValue = requestParam.getValue();
                if (!paramName.endsWith(".old")) {
                    final String paramValueOld = allRequestParams.get(paramName+".old");
                    if (!Objects.equals(paramValue, paramValueOld)) {
                        values.put(paramName, new OldNewValue(paramValueOld,paramValue));
                    }
                }
            }

            final StringBuilder sql = new StringBuilder();
            final List<Object> sqlParams = new ArrayList<>();

            if (values.size()>0) {
                sql.append("UPDATE issues SET ");
                boolean first = true;
                for (final Map.Entry<String,OldNewValue> value : values.entrySet()) {
                    if (first) first = false; else sql.append(",");
                    sql.append(value.getKey()+" = ?");
                    sqlParams.add(value.getValue().getNewValue());
                }

                sql.append(" WHERE id = ?");
                sqlParams.add(issueId);
                for (final Map.Entry<String,OldNewValue> value : values.entrySet()) {
                    sql.append(" AND (0=1");
                    if (StringUtils.isEmpty(value.getValue().getOldValue())) {
                        sql.append(" OR " + value.getKey() + " IS NULL OR " + value.getKey() + " = ''");
                    } else {
                        sql.append(" OR " + value.getKey() + " = ?");
                        sqlParams.add(value.getValue().getOldValue());
                    }
                    // it is OK if the field already has the new value (someone else changed it while this user was editing)
                    if (StringUtils.isEmpty(value.getValue().getNewValue())) {
                        sql.append(" OR " + value.getKey() + " IS NULL OR " + value.getKey() + " = ''");
                    } else {
                        sql.append(" OR " + value.getKey() + " = ?");
                        sqlParams.add(value.getValue().getNewValue());
                    }
                    sql.append(")");
                }

                final int updatedRecords = jdbcTemplate.update(sql.toString(), sqlParams.toArray(new Object[sqlParams.size()]));
                if (updatedRecords == 0) {
                    final String commaSeparatedFields = getCommaSeparatedFields(values);
                    final String selectFieldsSql = "SELECT " + commaSeparatedFields + " FROM " + TABLE + " WHERE id = ?";
                    final Map<String,Object> currentIssue = jdbcTemplate.queryForMap(selectFieldsSql, issueId);
                    final Map<String,Object> currentIssueCaseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    currentIssueCaseInsensitive.putAll(currentIssue);
                    for (final Map.Entry<String,OldNewValue> valueEntry : values.entrySet()) {
                        final String valueKey = valueEntry.getKey();
                        final Object currentValue = currentIssueCaseInsensitive.get(valueKey);
                        result.rejectValue(valueKey,"conflict", new Object[]{currentValue}, "Conflict");
                    }
                    if (!result.hasErrors()) {
                        throw new RuntimeException("There was no update, but no conflicts were found");
                    }
                    return VIEWS_ISSUE_CREATE_OR_UPDATE_FORM;
                } else if (updatedRecords > 1) {
                    throw new RuntimeException("More than one record updated. This was not expected");
                }
            }
            return "redirect:/issues/{issueId}";
        }
    }

    public static String getCommaSeparatedFields(final Map<String, ?> values) {
        final StringBuilder commaSeparatedFields = new StringBuilder();
        boolean first = true;
        for (final Map.Entry<String, ?> value : values.entrySet()) {
            if (first) first = false; else commaSeparatedFields.append(",");
            commaSeparatedFields.append(value.getKey());
        }
        return commaSeparatedFields.toString();
    }

    /**
     * Custom handler for displaying an issue.
     *
     * @param issueId the ID of the issue to display
     * @return a ModelMap with the model attributes for the view
     */
    @GetMapping("/issues/{issueId}")
    public ModelAndView showIssue(@PathVariable("issueId") int issueId) {
        ModelAndView mav = new ModelAndView("issues/issueDetails");
        mav.addObject(this.issues.findById(issueId));
        return mav;
    }

}
