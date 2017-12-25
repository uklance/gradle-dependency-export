package com.lazan.dependency.export;

import java.io.File;

public interface ModelResolveListener {
	void onResolveModel(String groupId, String artifactId, String version, File pomFile);
}
