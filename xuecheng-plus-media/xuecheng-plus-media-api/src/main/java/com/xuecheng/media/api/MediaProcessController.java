package com.xuecheng.media.api;


import com.xuecheng.media.service.IMediaProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author CCL
 * @since 2024-07-28
 */
@RestController
@RequestMapping("/media-process")
public class MediaProcessController {

    @Autowired
    private IMediaProcessService mediaProcessService;


}
