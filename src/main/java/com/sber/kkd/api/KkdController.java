package com.sber.kkd.api;

import com.sber.kkd.api.dto.IssuesPageDto;
import com.sber.kkd.api.dto.MetaDto;
import com.sber.kkd.api.dto.RunDto;
import com.sber.kkd.api.dto.RuleRefDto;
import com.sber.kkd.api.dto.SummaryDto;
import com.sber.kkd.api.exception.ApiException;
import com.sber.kkd.service.IssueService;
import com.sber.kkd.service.IssuesRequest;
import com.sber.kkd.service.KkdService;
import com.sber.kkd.service.MetaService;
import com.sber.kkd.service.RunService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class KkdController {

    private static final int MAX_LIMIT = 100;

    private final RunService runService;
    private final KkdService kkdService;
    private final IssueService issueService;
    private final MetaService metaService;

    public KkdController(RunService runService, KkdService kkdService, IssueService issueService,
                         MetaService metaService) {
        this.runService = runService;
        this.kkdService = kkdService;
        this.issueService = issueService;
        this.metaService = metaService;
    }

    @GetMapping("/meta")
    public MetaDto meta() {
        return new MetaDto(
                metaService.units(),
                metaService.rules().stream()
                        .map(r -> new RuleRefDto(r.getCode(), r.getName(), r.getSeverity()))
                        .toList()
        );
    }

    @GetMapping("/runs")
    public List<RunDto> runs() {
        return runService.listRuns();
    }

    @GetMapping("/kkd/summary")
    public SummaryDto summary(@RequestParam String run_id,
                              @RequestParam String unit_id) {
        return kkdService.summary(run_id, unit_id);
    }

    @GetMapping("/kkd/issues")
    public IssuesPageDto issues(@RequestParam String run_id,
                                @RequestParam(required = false) String rule,
                                @RequestParam(required = false) String severity,
                                @RequestParam(required = false) String unit,
                                @RequestParam(required = false) String delta,
                                @RequestParam(required = false, defaultValue = "20") Integer limit,
                                @RequestParam(required = false, defaultValue = "0") Integer offset) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw ApiException.badParam("limit должен быть в диапазоне 1.." + MAX_LIMIT);
        }
        if (offset < 0) {
            throw ApiException.badParam("offset не может быть отрицательным");
        }
        IssuesRequest request = new IssuesRequest(rule, severity, unit, delta);
        return issueService.issues(run_id, request, limit, offset);
    }
}