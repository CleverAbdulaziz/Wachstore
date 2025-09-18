package com.wristwatch.shop.bot;

import com.wristwatch.shop.dto.CategoryDto;
import com.wristwatch.shop.dto.ProductCreateRequest;
import lombok.Data;

@Data
public class AdminSession {

    private AdminAction currentAction;
    private ProductStep productStep;
    private CategoryStep categoryStep;
    private ProductCreateRequest tempProduct;
    private CategoryDto tempCategory;

}
