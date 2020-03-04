package vsii;


import com.filenet.api.admin.StorageArea;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.*;
import com.filenet.api.core.*;
import com.filenet.api.property.Properties;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Provides an abstract class that is extended to create a class implementing
 * each service provided by the plug-in. Services are actions, similar to
 * servlets or Struts actions, that perform operations on the IBM Content
 * Navigator server. A service can access content server application programming
 * interfaces (APIs) and Java EE APIs.
 * <p>
 * Services are invoked from the JavaScript functions that are defined for the
 * plug-in by using the <code>ecm.model.Request.invokePluginService</code>
 * function.
 * </p>
 * Follow best practices for servlets when implementing an IBM Content Navigator
 * plug-in service. In particular, always assume multi-threaded use and do not
 * keep unshared information in instance variables.
 */
public class CreateFolderService extends PluginService {

    private static String CONTENT_FILE = "endFile";
    private static String TYPE_FILE = "endType";
    private static String NAME_FILE = "endName";

    /**
     * Returns the unique identifier for this service.
     * <p>
     * <strong>Important:</strong> This identifier is used in URLs so it must
     * contain only alphanumeric characters.
     * </p>
     *
     * @return A <code>String</code> that is used to identify the service.
     */
    public String getId() {
        return "CreateFolderService";
    }

    /**
     * Returns the name of the IBM Content Navigator service that this service
     * overrides. If this service does not override an IBM Content Navigator
     * service, this method returns <code>null</code>.
     *
     * @returns The name of the service.
     */
    public String getOverriddenService() {
        return null;
    }

    /**
     * Performs the action of this service.
     *
     * @param callbacks An instance of the <code>PluginServiceCallbacks</code> class
     *                  that contains several functions that can be used by the
     *                  service. These functions provide access to the plug-in
     *                  configuration and content server APIs.
     * @param request   The <code>HttpServletRequest</code> object that provides the
     *                  request. The service can access the invocation parameters from
     *                  the request.
     * @param response  The <code>HttpServletResponse</code> object that is generated
     *                  by the service. The service can get the output stream and
     *                  write the response. The response must be in JSON format.
     * @throws Exception For exceptions that occur when the service is running. If the
     *                   logging level is high enough to log errors, information about
     *                   the exception is logged by IBM Content Navigator.
     */
    public void execute(PluginServiceCallbacks callbacks,
                        HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String action = request.getParameter("stringAction");
        String messageResponse = "";
        JSONObject conten = new JSONObject();
        ObjectStore objectStore = null;
        try {
            objectStore = getObjectStore();
            messageResponse = "get ObjectStore thành công";
            switch (action) {
                case "createFolder":
                    String nameFolder = request.getParameter("nameFolder");
                    String nameSubFolder = request.getParameter("nameSubFolder");
                    String nameFile = request.getParameter("nameFile");
                    String content = request.getParameter("content");
                    String typeDocument = request.getParameter("typeDocument");
                    Folder createNewFolder = createFolder(objectStore, nameFolder, nameSubFolder);
                    String[] listName = nameFile.split(NAME_FILE);
                    String[] listType = typeDocument.split(TYPE_FILE);
                    String[] contentFile = content.split(CONTENT_FILE);

                    for (int i = 0; i < listType.length; i++) {
                        upFile(listName[i], objectStore, createNewFolder, contentFile[i], listType[i]);
                    }
                    break;
                case "copyFolder":
                    String nameFolderCopy = request.getParameter("nameFolderCopy");
                    String newNameFolder = request.getParameter("newNameFolder");
                    Folder folderCopy = getFolderParent(objectStore, nameFolderCopy);
                    if (folderCopy == null) {
                        messageResponse += " --- can not found folder";
                    } else {
                        Folder newFolder = createFolder(objectStore, newNameFolder, null);
                        copyFolder(folderCopy, newFolder, objectStore);
                        messageResponse += " --- copy folder success";
                    }
                    break;
                case "deleteFolder":
                    String nameFolderDelete = request.getParameter("nameFolderDelete");
                    Folder folderDelete = getFolderParent(objectStore, nameFolderDelete);
                    if (folderDelete == null) {
                        messageResponse += " --- can not found folder";
                    } else {
                        deleteFolder(folderDelete);
                        messageResponse += " --- The folder " + nameFolderDelete + " has been deleted ";
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            messageResponse = "get ObjectStore thất bại";
        }
        conten.put("messageResponse", messageResponse);
        conten.serialize(response.getOutputStream());
    }

    private void deleteFolder(Folder folderDelete) {
        FolderSet subFolder = folderDelete.get_SubFolders();
        if (subFolder.isEmpty()) {
            folderDelete.delete();
            folderDelete.save(RefreshMode.REFRESH);
        } else {
            Iterator<Folder> iterator = subFolder.iterator();
            Folder folder = null;
            while (iterator.hasNext()) {
                folder = iterator.next();
                deleteSubFolder(folder);
                folder.delete();
                folder.save(RefreshMode.REFRESH);
            }
        }
    }

    private void deleteSubFolder(Folder folder) {
        FolderSet subFolder = folder.get_SubFolders();
        if (subFolder.isEmpty()) {
            folder.delete();
            folder.save(RefreshMode.REFRESH);
        } else {
            Iterator iterator = subFolder.iterator();
            Folder f = null;
            while (iterator.hasNext()) {
                f = (Folder) iterator.next();
                deleteSubFolder(f);
                f.delete();
                f.save(RefreshMode.REFRESH);
            }
        }
    }

    private void copyFolder(Folder folderCopy, Folder folderNew, ObjectStore objectStore) {
        FolderSet subFolder = folderCopy.get_SubFolders();
        Iterator<Folder> iterator = subFolder.iterator();
        Folder folder = null;
        DocumentSet folderSet = folderCopy.get_ContainedDocuments();
        Iterator it = folderSet.iterator();
        while (it.hasNext()) {
            Document document = (Document) it.next();
            createFile(folderNew, document, objectStore);
        }
        while (iterator.hasNext()) {
            folder = iterator.next();
            generateFolder(folder, objectStore, folderNew);
        }
    }

    private void generateFolder(Folder folderCopy, ObjectStore os, Folder folderNew) {
        Folder folderFather = Factory.Folder.createInstance(os, folderCopy.getClassName());
        folderFather.set_Parent(folderNew);
        folderFather.set_FolderName(folderCopy.get_FolderName());
        folderFather.save(RefreshMode.REFRESH);

        DocumentSet folderSet = folderCopy.get_ContainedDocuments();
        Iterator it = folderSet.iterator();
        while (it.hasNext()) {
            Document document = (Document) it.next();
            createFile(folderFather, document, os);
        }
        FolderSet subFolder = folderCopy.get_SubFolders();
        Iterator iterator = subFolder.iterator();
        Folder folder = null;
        while (iterator.hasNext()) {
            folder = (Folder) iterator.next();
            generateFolder(folder, os, folderFather);
        }
    }

    private void createFile(Folder folder, Document document, ObjectStore os) {
        Document doc = Factory.Document.createInstance(os, document.getClassName());
        ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
        ContentElementList contentElementList = Factory.ContentElement.createList();
        contentTransfer.setCaptureSource(document.accessContentStream(0));
        contentElementList.add(contentTransfer);
        doc.set_ContentElements(contentElementList);
        contentTransfer.set_RetrievalName(document.get_Name());
        contentTransfer.set_ContentType(document.get_MimeType());
        doc.set_MimeType(document.get_MimeType());
        doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
        doc.save(RefreshMode.REFRESH);
        ReferentialContainmentRelationship rc = folder
                .file(doc, AutoUniqueName.AUTO_UNIQUE, document.get_Name(),
                        DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
        rc.save(RefreshMode.REFRESH);
    }

    private Folder getFolderParent(ObjectStore objectStore, String nameFolderCopy) {
        Folder folderCopy = null;
        String mySQLString = "SELECT * FROM Folder WHERE FolderName = '" + nameFolderCopy + "'";
        SearchSQL sqlObject = new SearchSQL();
        sqlObject.setQueryString(mySQLString);
        SearchScope searchScope = new SearchScope(objectStore);
        IndependentObjectSet objSet = searchScope.fetchObjects(sqlObject, null, null, Boolean.TRUE);
        Iterator it = objSet.iterator();
        while (it.hasNext()) {
            folderCopy = (Folder) it.next();
            break;
        }
        return folderCopy;
    }

    private void upFile(String nameFile, ObjectStore os, Folder folder, String content, String typeDocument) {
        byte[] imageByte = DatatypeConverter.parseBase64Binary(content);
        InputStream is = new ByteArrayInputStream(imageByte);
        Document doc = Factory.Document.createInstance(os, "Document");
        ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
        ContentElementList contentElementList = Factory.ContentElement.createList();
        contentTransfer.setCaptureSource(is);
        contentElementList.add(contentTransfer);
        doc.set_ContentElements(contentElementList);
        contentTransfer.set_RetrievalName(nameFile);
        contentTransfer.set_ContentType(typeDocument);
        doc.set_MimeType(typeDocument);
        doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
        Properties prop = doc.getProperties();
        prop.putValue("DocumentTitle", nameFile);
        doc.save(RefreshMode.REFRESH);
        ReferentialContainmentRelationship rc = folder
                .file(doc, AutoUniqueName.AUTO_UNIQUE, nameFile,
                        DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
        rc.save(RefreshMode.REFRESH);
    }

    private Folder createFolder(ObjectStore objectStore, String nameFolder, String nameSubFolder) {
        Folder folderFather = Factory.Folder.createInstance(objectStore, "Folder");
        Folder rootFolder = objectStore.get_RootFolder();
        folderFather.set_Parent(rootFolder);
        folderFather.set_FolderName(nameFolder);
        folderFather.save(RefreshMode.REFRESH);

        if (nameSubFolder != null && !nameFolder.isEmpty()) {
            Folder subFolder = folderFather.createSubFolder(nameSubFolder);
            subFolder.save(RefreshMode.REFRESH);
            return subFolder;
        }
        return folderFather;
    }

    public ObjectStore getObjectStore() {
        String uri = "http://192.168.0.99:9080/wsi/FNCEWS40MTOM/";
        String username = "p8admin";
        String password = "Admin123";

        Connection conn = Factory.Connection.getConnection(uri);
        UserContext uc = UserContext.get();
        ObjectStore os = null;
        uc.pushSubject(
                UserContext.createSubject(conn, username, password, "FileNetP8WSI")
        );
        try {
            Domain domain = Factory.Domain.getInstance(conn, null);
            os = Factory.ObjectStore.fetchInstance(domain,
                    "TARGET", null);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return os;
    }

    private void createDocument(ObjectStore objectStore, String nameDoc, JSONObject conten) {
        Document document = Factory.Document.createInstance(objectStore, "Document");
        document.getProperties().putValue("DocumentTitle", "New Document via Java API");
        document.set_MimeType("text/plain");
        StorageArea sa = Factory.StorageArea.getInstance(objectStore, new Id("{DE42374D-B04B-4F47-A62E-CAC9AC9A5719}"));
        document.set_StorageArea(sa);
        document.save(RefreshMode.NO_REFRESH);
    }


}
