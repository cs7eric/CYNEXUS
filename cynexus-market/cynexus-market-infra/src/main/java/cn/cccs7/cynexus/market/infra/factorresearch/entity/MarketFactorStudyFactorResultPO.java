package cn.cccs7.cynexus.market.infra.factorresearch.entity;

import lombok.Data;

@Data
public class MarketFactorStudyFactorResultPO {
    private String discoveryCode;
    private int recipeVersion;
    private String method;
    private String scope;
    private String recipe;
    private String learnedBounds;
    private String statistics;
    private String validation;
    private String evidence;
    private String verdict;
    private int rank;
}
