package com.loanapp.dto;
import com.loanapp.util.AppConstants;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanSearchRequest {

    private int page = AppConstants.DEFAULT_PAGE_NUMBER;

    private int size = AppConstants.DEFAULT_PAGE_SIZE;

    private String sortBy = AppConstants.DEFAULT_SORT_BY;

    private String direction = AppConstants.DEFAULT_SORT_DIRECTION;

    private String search;

}
