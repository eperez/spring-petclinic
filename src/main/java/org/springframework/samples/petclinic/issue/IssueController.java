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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.Collection;
import java.util.Map;

@Controller
class IssueController {

    private static final String VIEWS_ISSUE_CREATE_OR_UPDATE_FORM = "issues/createOrUpdateIssueForm";
    private final IssueRepository issues;


    public IssueController(IssueRepository clinicService) {
        this.issues = clinicService;
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
    public String processUpdateIssueForm(@Valid Issue issue, BindingResult result, @PathVariable("issueId") int issueId) {
        if (result.hasErrors()) {
            return VIEWS_ISSUE_CREATE_OR_UPDATE_FORM;
        } else {
            issue.setId(issueId);
            this.issues.save(issue);
            return "redirect:/issues/{issueId}";
        }
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
