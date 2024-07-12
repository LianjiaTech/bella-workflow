package com.ke.bella.workflow.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ke.bella.workflow.db.repo.Page;
import com.ke.bella.workflow.service.DatasetService;

@RestController
@RequestMapping("/console/api/datasets")
public class DifyDatasetController {

    @Autowired
    DatasetService datasetService;

    @GetMapping
    public Page<DatasetService.Dataset> page(DatasetOps.DatasetPage page) {
        return datasetService.pageDataset(page);
    }
}
