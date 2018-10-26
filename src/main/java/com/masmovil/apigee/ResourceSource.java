package com.masmovil.apigee;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceSource {
    private int port;
    private String host;
    private String path;
}
