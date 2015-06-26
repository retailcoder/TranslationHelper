package de.vogel612.helper.ui;

import java.nio.file.Path;
import java.util.List;

import de.vogel612.helper.data.Translation;

public interface OverviewModel {

	void register(OverviewPresenter p);

	void loadFromDirectory(Path resxFolder, String targetLocale);

	List<Translation> getTranslations();

	Translation getSingleTranslation(String key);

	void updateTranslation(String key, String newTranslation);

	void save();
}
