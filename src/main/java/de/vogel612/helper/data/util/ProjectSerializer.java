package de.vogel612.helper.data.util;

import de.vogel612.helper.data.Project;
import de.vogel612.helper.data.ResourceSet;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Static helper class to serialize and deserialize Projects into and from files
 */
public final class ProjectSerializer {

    /**
     * Serializes a given Project into the given file
     *
     * @param project
     *         The project to serialize. Must not be null
     * @param file
     *         The file to serialize the project to. Must point to a file
     *
     * @throws IOException
     *         If the file could not be opened, created, written to or generally if IO went sideways
     */
    public static void serialize(Project project, Path file) throws IOException {
        Objects.requireNonNull(project, "project");
        // TODO check path for being a file

        Document resultDocument = new Document();
        resultDocument.setRootElement(getRoot(project.getName()));
        project.getAssociatedResources().stream().map(ProjectSerializer::getResourceSetEl)
                .forEach(resultDocument.getRootElement()::addContent);
        Serialization.serializeDocument(resultDocument, file);
    }

    private static Element getResourceSetEl(ResourceSet resourceSet) {
        Element el = new Element("resource-set");
        el.setAttribute("name", resourceSet.getName());
        el.setAttribute("folder", resourceSet.getFolder().toString());
        for (String locale : resourceSet.getLocales()) {
            Element localeEl = new Element("locale");
            localeEl.setAttribute("name", locale);
            el.addContent(localeEl);
        }
        return el;
    }

    private static Element getRoot(String name) {
        Element el = new Element("project");
        el.setAttribute("name", name);
        return el;
    }

    /**
     * Deserializes a project from a given file. No error-checking whatsoever is performed as of now
     *
     * @param file
     *         The file where the serialized project is saved at
     *
     * @return A Project-instance that's equivalent to the Project instance originally serialized into the file
     */
    public static Project deserialize(Path file) throws IOException {
        Document projectDocument = Serialization.parseFile(file);
        Element root = projectDocument.getRootElement();
        final String projectName = root.getAttribute("name").getValue();
        List<ResourceSet> declaredResources = root.getChildren().stream().map(ProjectSerializer::deserializeResourceSet)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new Project(projectName, declaredResources);
    }

    private static ResourceSet deserializeResourceSet(Element element) {
        final String name = element.getAttributeValue("name");
        final Path folder = Paths.get(element.getAttributeValue("folder"));
        final Set<String> locales = element.getChildren().stream()
                .map(el -> el.getAttributeValue("name"))
                .collect(Collectors.toSet());
        return new ResourceSet(name, folder, locales);
    }
}
