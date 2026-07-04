package com.imprint.dto;

import lombok.Data;

@Data
public class DeleteCategoryRequest {

    /** true=删除该分类下所有照片，false=保留照片并移至「其他」 */
    private boolean deletePhotos;
}
