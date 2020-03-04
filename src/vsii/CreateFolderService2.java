package vsii;

import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.*;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.UserContext;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

public class CreateFolderService2 extends PluginService {

    private static String CUSTOMER = "Khách hàng";
    private static String DVKH = "4. DVKH";
    private static String POS001 = "4.1 POS001";
    private static String NUMBER_CIF = "NAM12345";

    @Override

    public String getId() {
        return null;
    }

    @Override
    public void execute(PluginServiceCallbacks pluginServiceCallbacks, HttpServletRequest request,
                        HttpServletResponse response) throws Exception {

        NUMBER_CIF = request.getParameter("nameFolder");

        String messageResponse = "";
        JSONObject conten = new JSONObject();
        ObjectStore objectStore = null;
        try {
            objectStore = getObjectStore();
            messageResponse = "get ObjectStore thành công";

            // Folder Khách hàng
            Folder customerFolder = getFolderByName(objectStore, CUSTOMER);

            // 4. DVKH
            Folder folderDVKH = null;

            // 4.1 POS001
            Folder folderPOS001 = null;

            // folder cần tìm theo số Cif
            Folder folder = getFolderByName(objectStore, NUMBER_CIF);
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


        } catch (Exception e) {
            messageResponse = "get ObjectStore thất bại";
        }
        conten.put("messageResponse", messageResponse);
        conten.serialize(response.getOutputStream());
    }

    private Folder createNewFolder(ObjectStore objectStore, Folder folderFather, String nameFolder, String classId) {
        Folder folder = Factory.Folder.createInstance(objectStore, classId);
        folder.set_Parent(folderFather);
        folder.set_FolderName(nameFolder);
        folder.save(RefreshMode.REFRESH);
        return folder;
    }

    private Folder checkFolderDVKH(Folder folderCustomer, ObjectStore objectStore) {
        Folder folderDVKH = null;
        FolderSet subFolder = folderCustomer.get_SubFolders();
        Iterator<Folder> iterator = subFolder.iterator();
        Folder folder = null;
        while (iterator.hasNext()) {
            folder = iterator.next();
            if (folder.get_FolderName().equals(DVKH)) {
                folderDVKH = folder;
                break;
            }
        }
        return folderDVKH;
    }

    private Folder checkFolderPOS001(Folder folderDVKH, ObjectStore objectStore) {
        Folder folderPOS001 = null;
        FolderSet subFolder = folderDVKH.get_SubFolders();
        Iterator<Folder> iterator = subFolder.iterator();
        Folder folder = null;
        while (iterator.hasNext()) {
            folder = iterator.next();
            if (folder.get_FolderName().equals(POS001)) {
                folderPOS001 = folder;
                break;
            }
        }
        return folderPOS001;
    }

    private Folder getFolderByName(ObjectStore objectStore, String nameFolder) {
        Folder folder = null;
        String mySQLString = "SELECT * FROM Folder WHERE FolderName = '" + nameFolder + "'";
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
