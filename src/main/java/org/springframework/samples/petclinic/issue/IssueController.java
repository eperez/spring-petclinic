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

import name.fraser.neil.plaintext.diff_match_patch;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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
    private final diff_match_patch diff_match_patch = new diff_match_patch();
    {
        diff_match_patch.Match_Threshold = 0.1f;
    }

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
            final Map<String,LinkedList<diff_match_patch.Patch>> values = new HashMap<>();

            final Collection<String> fieldsEditAllowed = Arrays.asList("title","description");

            for (final String paramName : fieldsEditAllowed) {
                String paramValue = allRequestParams.get(paramName);
                if (paramValue == null) paramValue = "";
                paramValue = paramValue.replaceAll("\r","");
                String paramValueOld = allRequestParams.get(paramName+".old");
                if (paramValueOld == null) paramValueOld = "";
                paramValueOld = paramValueOld.replaceAll("\r","");
                if (!Objects.equals(paramValue, paramValueOld)) {
                    final LinkedList<diff_match_patch.Patch> patches = diff_match_patch.patch_make(paramValueOld, paramValue);
                    if (patches != null && !patches.isEmpty()) {
                        values.put(paramName, patches);
                    }
                }
            }

            if (values.size() > 0) {
                final String commaSeparatedFields = getCommaSeparatedFields(values);
                final String selectFieldsSql = "SELECT " +
                    commaSeparatedFields +
                    " FROM " + TABLE + " WHERE id = ?";
                final Map<String,Object> currentIssue = jdbcTemplate.queryForMap(selectFieldsSql, issueId);
                final Map<String,Object> currentIssueCaseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                currentIssueCaseInsensitive.putAll(currentIssue);


                final StringBuilder sql = new StringBuilder();
                final List<Object> sqlParams = new ArrayList<>();

                sql.append("UPDATE " + TABLE + " SET ");
                boolean first = true;
                for (final Map.Entry<String,LinkedList<diff_match_patch.Patch>> value : values.entrySet()) {
                    if (first) first = false; else sql.append(",");
                    sql.append(value.getKey()+" = ?");
                    final LinkedList<diff_match_patch.Patch> patches = value.getValue();
                    final Object currentValue = currentIssueCaseInsensitive.get(value.getKey());
                    final Object[] objects = diff_match_patch.patch_apply(patches, (String) currentValue);
                    if (areAllTrue((boolean[])objects[1])) {
                        final String patched = (String) objects[0];
                        sqlParams.add(patched);
                    } else {
                        result.rejectValue(value.getKey(),"conflict", new Object[]{currentValue}, "Conflict");
                    }
                }

                sql.append(" WHERE id = ?");
                sqlParams.add(issueId);
                for (final Map.Entry<String,LinkedList<diff_match_patch.Patch>> value : values.entrySet()) {
                    final Object currentValue = currentIssueCaseInsensitive.get(value.getKey());
                    if (StringUtils.isEmpty(currentValue)) {
                        sql.append(" AND (" + value.getKey() + " IS NULL OR " + value.getKey() + " = '')");
                    } else {
                        sql.append(" AND " + value.getKey() + " = ?");
                        sqlParams.add(currentValue);
                    }
                }

                if (result.hasErrors()) {
                    return VIEWS_ISSUE_CREATE_OR_UPDATE_FORM;
                }

                final int updatedRecords = jdbcTemplate.update(sql.toString(), sqlParams.toArray(new Object[sqlParams.size()]));
                if (updatedRecords == 0) {
                    for (final Map.Entry<String,LinkedList<diff_match_patch.Patch>> valueEntry : values.entrySet()) {
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

    public static boolean areAllTrue(boolean[] array) {
        for (boolean b : array) if (!b) return false;
        return true;
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
