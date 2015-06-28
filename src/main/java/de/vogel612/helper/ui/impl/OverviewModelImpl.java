package de.vogel612.helper.ui.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import de.vogel612.helper.data.Translation;
import de.vogel612.helper.ui.OverviewModel;
import de.vogel612.helper.ui.OverviewPresenter;

public class OverviewModelImpl implements OverviewModel {

	private static final String VALUE_NAME = "value";

	private static final String KEY_NAME = "name";

	private static final String ELEMENT_NAME = "data";

	private static final String FILE_NAME_FORMAT = "RubberduckUI%s.resx";

	private final Map<String, String> originalLocale = new HashMap<String, String>();
	private final ExecutorService fileOperationService = Executors
			.newSingleThreadExecutor();

	private final BiConsumer<Path, Boolean> loadResxFile = (path, isTarget) -> {
		SAXBuilder documentBuilder = new SAXBuilder();
		try {
			if (isTarget) {
				this.translationDocument = documentBuilder.build(path.toFile());
			} else {
				Document doc = documentBuilder.build(path.toFile());
				List<Element> translationElements = doc.getRootElement()
						.getChildren(ELEMENT_NAME);

				translationElements.stream().forEach(
						element -> {
							originalLocale.put(element.getAttribute(KEY_NAME)
									.getValue(), element
									.getChildText(VALUE_NAME));
						});
			}
		} catch (JDOMException e) {
			this.presenter.onException(e, "Unspecified Parsing error");
		} catch (IOException e) {
			this.presenter.onException(e, "Unspecified I/O Error");
		} catch (Exception e) {
			this.presenter.onException(e, "Something went really wrong");
		}
	};
	private final XPathFactory xPathFactory = XPathFactory.instance();

	private OverviewPresenter presenter;
	private Document translationDocument;
	private Path currentPath;
	private String currentTargetLocale;

	@Override
	public void register(final OverviewPresenter p) {
		presenter = p;
	}

	@Override
	public void loadFromDirectory(final Path resxFolder,
			final String targetLocale) {
		this.currentPath = resxFolder;
		this.currentTargetLocale = targetLocale;
		// for now there's only en-US root-Files
		final Path rootFile = resxFolder.resolve(fileNameString(""));
		final Path targetFile = resxFolder
				.resolve(fileNameString(targetLocale));

		Runnable buildDocument = () -> {
			originalLocale.clear();
			loadResxFile.accept(rootFile, false);
			loadResxFile.accept(targetFile, true);
			normalizeTargetLocale();
			presenter.onParseCompletion();
		};
		fileOperationService.submit(buildDocument);
	}

	private void normalizeTargetLocale() {
		List<Element> translationElements = translationDocument
				.getRootElement().getChildren(ELEMENT_NAME);
		Set<String> passedKeys = new HashSet<String>();
		Iterator<Element> it = translationElements.iterator();

		while (it.hasNext()) {
			Element el = it.next();
			String key = el.getAttribute(KEY_NAME).getValue();
			if (!originalLocale.containsKey(key)) {
				// LIVE COLLECTION!!
				it.remove();
				continue;
			}
			passedKeys.add(key);
		}

		// build new elements for newly created keys in root
		originalLocale
				.keySet()
				.stream()
				.filter(k -> !passedKeys.contains(k))
				.forEach(
						k -> {
							Element newElement = new Element(ELEMENT_NAME);
							Element valueContainer = new Element(VALUE_NAME);
							valueContainer.setText(originalLocale.get(k));

							newElement.setAttribute(KEY_NAME, k);
							newElement.addContent(valueContainer);
							translationDocument.getRootElement().addContent(
									newElement);
						});
	}

	private String fileNameString(final String localeIdent) {
		return String.format(FILE_NAME_FORMAT, localeIdent.isEmpty() ? "" : "."
				+ localeIdent.toLowerCase());
	}

	@Override
	public List<Translation> getTranslations() {
		List<Element> translationElements = translationDocument
				.getRootElement().getChildren(ELEMENT_NAME);

		return translationElements.stream().map(el -> {
			final String key = el.getAttribute(KEY_NAME).getValue();
			final String currentValue = el.getChildText(VALUE_NAME);
			return new Translation(key, originalLocale.get(key), currentValue);
		}).collect(Collectors.toList());
	}

	@Override
	public void updateTranslation(final String key, final String newTranslation) {
		Element translationToUpdate = getValueElement(key);
		translationToUpdate.setText(newTranslation);
	}

	private Element getValueElement(final String key) {
		XPathExpression<Element> expression = xPathFactory.compile("/*/"
				+ ELEMENT_NAME + "[@" + KEY_NAME + "='" + key + "']/"
				+ VALUE_NAME, Filters.element());
		// ohh damn that's so many assumptions
		Element element = expression.evaluate(translationDocument).get(0);
		return element;
	}

	@Override
	public void save() {
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		Path outFile = currentPath.resolve(fileNameString(currentTargetLocale));

		try (OutputStream outStream = Files.newOutputStream(outFile,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			outputter.output(translationDocument, outStream);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			presenter.onException(e, "Could not save File");
		}
	}
	@Override
	public Translation getSingleTranslation(final String key) {
		final String currentValue = getValueElement(key).getText();
		return new Translation(key, originalLocale.get(key), currentValue);
	}

}
