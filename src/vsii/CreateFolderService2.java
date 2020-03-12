package vsii;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.*;
import com.filenet.api.core.*;
import com.filenet.api.property.Properties;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.UserContext;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONObject;
import com.sun.deploy.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CreateFolderService2 extends PluginService {

    private static String CUSTOMER = "Khách hàng";
    private static String DVKH = "4. DVKH";
    private static String POS001 = "4.1 POS001";

    @Override

    public String getId() {
        return null;
    }

    @Override
    public void execute(PluginServiceCallbacks pluginServiceCallbacks, HttpServletRequest request,
                        HttpServletResponse response) throws Exception {

        String NUMBER_CIF = request.getParameter("nameFolder");

        String messageResponse;
        JSONObject conten = new JSONObject();
        ObjectStore objectStore;
        try {
            objectStore = getObjectStore();
            messageResponse = "get ObjectStore thành công";

            // Folder Khách hàng
            Folder customerFolder = getFolderByName(objectStore, CUSTOMER, "Folder");

            // 4. DVKH
            Folder folderDVKH;

            // 4.1 POS001
            Folder folderPOS001;

            // folder cần tìm theo số Cif
            Folder folder = getFolderByName(objectStore, NUMBER_CIF, "CIFModel");
            if (folder != null) {
                folderDVKH = checkFolderDVKH(customerFolder, objectStore);
                if (folderDVKH != null) {
                    folderPOS001 = checkFolderPOS001(folderDVKH, objectStore);
                    if (folderPOS001 == null) {
                        // chưa có folder POS001 ===> tạo folder POS001
                        folderPOS001 = createNewFolder(objectStore, folderDVKH, POS001, "POSClass");
                    }
                } else {
                    folderDVKH = createNewFolder(objectStore, folder, DVKH, "CustomerService");
                    folderPOS001 = createNewFolder(objectStore, folderDVKH, POS001, "POSClass");
                }
            } else {
                folder = createNewFolder(objectStore, customerFolder, NUMBER_CIF, "CIFModel");
                folderDVKH = createNewFolder(objectStore, folder, DVKH, "CustomerService");
                folderPOS001 = createNewFolder(objectStore, folderDVKH, POS001, "POSClass");
            }

            // List đường dẫn file truyền vào
            List<String> paths = new ArrayList<>();
            String CLASSID_FILE = "ClassID12345";
            upload(objectStore, folderPOS001, CLASSID_FILE, paths);

        } catch (Exception e) {
            messageResponse = "get ObjectStore thất bại";
        }
        conten.put("messageResponse", messageResponse);
        conten.serialize(response.getOutputStream());
    }

    /**
     * @param folderFather Folder chức folder định tạo
     * @param nameFolder   Tên folder định tạo
     * @param classId      classId folder định tạo
     */
    private Folder createNewFolder(ObjectStore objectStore, Folder folderFather, String nameFolder, String classId) {
        Folder folder = Factory.Folder.createInstance(objectStore, classId);
        folder.set_Parent(folderFather);
        folder.set_FolderName(nameFolder);
        folder.save(RefreshMode.REFRESH);
        return folder;
    }

    /**
     * @param folder   folder lưu file trên FileNet
     * @param is       file cần đẩy lên FileNet
     * @param nameFile tên name của File đẩy lên
     * @param typeFile type file của File đẩy lên
     * @param classId  classId của File đẩy lên
     */
    private void upFile(ObjectStore os, Folder folder, InputStream is, String nameFile, String typeFile, String classId) {
        Document doc = Factory.Document.createInstance(os, classId);
        ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
        ContentElementList contentElementList = Factory.ContentElement.createList();
        contentTransfer.setCaptureSource(is);
        contentElementList.add(contentTransfer);
        doc.set_ContentElements(contentElementList);
        contentTransfer.set_RetrievalName(nameFile);
        contentTransfer.set_ContentType(typeFile);
        doc.set_MimeType(typeFile);
        doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
        Properties prop = doc.getProperties();
        prop.putValue("DocumentTitle", nameFile);
        doc.save(RefreshMode.REFRESH);
        ReferentialContainmentRelationship rc = folder
                .file(doc, AutoUniqueName.AUTO_UNIQUE, nameFile,
                        DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
        rc.save(RefreshMode.REFRESH);
    }

    /**
     * @param folder  folder lưu file trên FileNet
     * @param classId classId của File đẩy lên
     * @param paths   list đường dẫn lấy file dưới local
     *                VD Paths "C:\\Users\\vcis\\Downloads\\CIF0000009.docx";
     */
    public void upload(ObjectStore os, Folder folder, String classId, List<String> paths) {
        for (String path : paths) {
            String[] folderPath = StringUtils.splitString(path, "\\\\");
            String nameFile = null;
            for (String str : folderPath) {
                nameFile = str;
            }
            String typeFile = StringUtils.splitString(nameFile, ".")[1];
            try {
                InputStream inputStream = new FileInputStream(path);
                upFile(os, folder, inputStream, nameFile, typeFile, classId);
            } catch (FileNotFoundException e) {
                System.out.println("-----------File not found----------");
                e.printStackTrace();
            }
        }
    }

    /**
     * Kiếm tra xem có folder DVKH có chưa
     *
     * @return nếu có folder DVKH rồi thì trả lại , chưa return null
     */
    private Folder checkFolderDVKH(Folder folderCustomer, ObjectStore objectStore) {
        Folder folderDVKH = null;
        FolderSet subFolder = folderCustomer.get_SubFolders();
        Iterator iterator = subFolder.iterator();
        Folder folder;
        while (iterator.hasNext()) {
            folder = (Folder) iterator.next();
            if (folder.get_FolderName().equals(DVKH)) {
                folderDVKH = folder;
                break;
            }
        }
        return folderDVKH;
    }

    /**
     * Kiếm tra xem có folder POS001 đã có trong folder DVKH chưa
     *
     * @return nếu có folder POS001 rồi thì trả lại , chưa return null
     */
    private Folder checkFolderPOS001(Folder folderDVKH, ObjectStore objectStore) {
        Folder folderPOS001 = null;
        FolderSet subFolder = folderDVKH.get_SubFolders();
        Iterator iterator = subFolder.iterator();
        Folder folder;
        while (iterator.hasNext()) {
            folder = (Folder) iterator.next();
            if (folder.get_FolderName().equals(POS001)) {
                folderPOS001 = folder;
                break;
            }
        }
        return folderPOS001;
    }

    /**
     * Tìm folder này đã có hay chưa
     *
     * @param nameFolder tên folder cần tìm
     * @param classId    classId của folder cần tìm
     * @return nếu có rồi thì trả lại folder cần tìm , chưa return null
     */
    private Folder getFolderByName(ObjectStore objectStore, String nameFolder, String classId) {
        Folder folder = null;
        String mySQLString = "SELECT * FROM" + classId + " WHERE FolderName = '" + nameFolder + "'";
        SearchSQL sqlObject = new SearchSQL();
        sqlObject.setQueryString(mySQLString);
        SearchScope searchScope = new SearchScope(objectStore);
        IndependentObjectSet objSet = searchScope.fetchObjects(sqlObject, null, null, Boolean.TRUE);
        Iterator it = objSet.iterator();
        while (it.hasNext()) {
            folder = (Folder) it.next();
            break;
        }
        return folder;
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
}
