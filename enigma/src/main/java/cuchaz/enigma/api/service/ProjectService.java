package cuchaz.enigma.api.service;

import cuchaz.enigma.api.view.ProjectView;

public interface ProjectService extends EnigmaService {
	EnigmaServiceType<ProjectService> TYPE = EnigmaServiceType.create("project");

	default void onProjectOpen(ProjectView project) {
	}

	default void onProjectClose(ProjectView project) {
	}
}
