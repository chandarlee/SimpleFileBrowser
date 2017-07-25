package netease.lcd;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Activity para escolha de arquivos/diretorios.
 *
 * @author android
 *
 */
public class FileDialog extends ListActivity implements View.OnClickListener, SimpleAdapter.ViewBinder{

    /**
     * Chave de um item da lista de paths.
     */
    private static final String ITEM_NAME = "name";

    /**
     * Imagem de um item da lista de paths (diretorio ou arquivo).
     */
    private static final String ITEM_FILE_TYPE = "file_type";

    private static final String ITEM_SELECTED = "selected";

    /**
     * Diretorio raiz.
     */
    private static final String ROOT = "/";

    /**
     * Parametro de entrada da Activity: path inicial. Padrao: ROOT.
     */
    public static final String START_PATH = "START_PATH";

    /**
     * Parametro de entrada da Activity: filtro de formatos de arquivos. Padrao:
     * null.
     */
    public static final String FORMAT_FILTER = "FORMAT_FILTER";

    /**
     * Parametro de saida da Activity: path escolhido. Padrao: null.
     */
    public static final String RESULT_PATH = "RESULT_PATH";

    /**
     * Parametro de entrada da Activity: tipo de selecao: pode criar novos paths
     * ou nao. Padrao: nao permite.
     *
     * @see {@link SelectionMode}
     */
    //public static final String SELECTION_MODE = "SELECTION_MODE";

    /**
     * Parametro de entrada da Activity: se e permitido escolher diretorios.
     * Padrao: falso.
     */
    public static final String CAN_SELECT_DIR = "CAN_SELECT_DIR";

    private List<String> path = new ArrayList<>();
    private TextView myPath;
    private EditText mFileName;
    private ArrayList<HashMap<String, Object>> mList = new ArrayList<>();

    private Button selectButton;

    private LinearLayout layoutSelect;
    private LinearLayout layoutCreate;
    private InputMethodManager inputManager;
    private String parentPath;
    private String currentPath = ROOT;

    //private int selectionMode = SelectionMode.MODE_CREATE;

    private String[] formatFilter = null;

    private boolean canSelectDir = false;

    private String selectedFile;
    private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

    SimpleAdapter mAdapter;

    /**
     * Called when the activity is first created. Configura todos os parametros
     * de entrada e das VIEWS..
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED, getIntent());

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.file_dialog_main);
        myPath = (TextView) findViewById(R.id.path);
        mFileName = (EditText) findViewById(R.id.fdEditTextFile);

        inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        selectButton = (Button) findViewById(R.id.fdButtonSelect);
        selectButton.setEnabled(false);
        selectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (selectedFile != null) {
                    getIntent().putExtra(RESULT_PATH, selectedFile);
                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            }
        });

        findViewById(R.id.fdNewDir).setOnClickListener(this);
        findViewById(R.id.fdNewFile).setOnClickListener(this);

        formatFilter = getIntent().getStringArrayExtra(FORMAT_FILTER);

        canSelectDir = getIntent().getBooleanExtra(CAN_SELECT_DIR, false);

        layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);
        layoutCreate = (LinearLayout) findViewById(R.id.fdLinearLayoutCreate);
        layoutCreate.setVisibility(View.GONE);

        final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
        cancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setSelectVisible(v);
            }

        });
        final Button createButton = (Button) findViewById(R.id.fdButtonCreate);
        createButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mFileName.getText().length() > 0) {

                    File file = new File(currentPath + "/" + mFileName.getText());
                    boolean success;
                    String message = null;
                    try {
                        success = mCreatingFile ? file.createNewFile() : file.mkdir();
                    } catch (IOException | SecurityException e) {
                        e.printStackTrace();
                        success = false;
                        message = e.getMessage();
                    }

                    if (success){
                        getDir(currentPath);
                        int position = path.indexOf(file.getAbsolutePath());
                        getListView().setSelection(position);
                        setSelectedStateForPosition(position, true);
                    }else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(FileDialog.this);
                        builder.setIcon(R.drawable.icon_alert)
                                .setTitle(mCreatingFile ? R.string.cannot_create_file : R.string.cannot_create_dir)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                });
                        if (message != null){
                            builder.setMessage(message);
                        }
                        builder.show();
                    }
                }
            }
        });

        String startPath = getIntent().getStringExtra(START_PATH);
        startPath = startPath != null ? startPath : ROOT;
        /*if (canSelectDir) {
            File file = new File(startPath);
            selectedFile = file;
            selectButton.setEnabled(true);
        }*/
        getDir(startPath);
    }

    private void getDir(String dirPath) {

        boolean useAutoSelection = dirPath.length() < currentPath.length();

        Integer position = lastPositions.get(parentPath);

        getDirImpl(dirPath);

        if (position != null && useAutoSelection) {
            getListView().setSelection(position);
        }

    }

    /**
     * Monta a estrutura de arquivos e diretorios filhos do diretorio fornecido.
     *
     * @param dirPath
     *            Diretorio pai.
     */
    private void getDirImpl(final String dirPath) {

        currentPath = dirPath;

        //final List<String> item = new ArrayList<String>();
        path.clear();
        mList.clear();

        File f = new File(currentPath);
        File[] files = f.listFiles();
        if (files == null) {
            currentPath = ROOT;
            f = new File(currentPath);
            files = f.listFiles();
        }
        myPath.setText(getString(R.string.location, currentPath));

        if (!currentPath.equals(ROOT)) {

            //item.add(ROOT);
            addItem(ROOT, true);
            path.add(ROOT);

            //item.add("../");
            addItem("../", true);
            path.add(f.getParent());
            parentPath = f.getParent();

        }

        TreeMap<String, String> dirsMap = new TreeMap<String, String>();
        TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
        TreeMap<String, String> filesMap = new TreeMap<String, String>();
        TreeMap<String, String> filesPathMap = new TreeMap<String, String>();
        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                dirsMap.put(dirName, dirName);
                dirsPathMap.put(dirName, file.getPath());
            } else {
                final String fileName = file.getName();
                final String fileNameLwr = fileName.toLowerCase();
                // se ha um filtro de formatos, utiliza-o
                if (formatFilter != null) {
                    boolean contains = false;
                    for (int i = 0; i < formatFilter.length; i++) {
                        final String formatLwr = formatFilter[i].toLowerCase();
                        if (fileNameLwr.endsWith(formatLwr)) {
                            contains = true;
                            break;
                        }
                    }
                    if (contains) {
                        filesMap.put(fileName, fileName);
                        filesPathMap.put(fileName, file.getPath());
                    }
                    // senao, adiciona todos os arquivos
                } else {
                    filesMap.put(fileName, fileName);
                    filesPathMap.put(fileName, file.getPath());
                }
            }
        }
        //item.addAll(dirsMap.tailMap("").values());
        //item.addAll(filesMap.tailMap("").values());
        path.addAll(dirsPathMap.tailMap("").values());
        path.addAll(filesPathMap.tailMap("").values());

        if (mAdapter != null){
            mAdapter.notifyDataSetInvalidated();
        }

        mAdapter = new SimpleAdapter(this, mList, R.layout.file_dialog_row, new String[] {
                ITEM_NAME, ITEM_FILE_TYPE, ITEM_SELECTED }, new int[] { R.id.fdrowtext, R.id.fdrowimage, R.id.checkedImage });
        mAdapter.setViewBinder(this);

        for (String dir : dirsMap.tailMap("").values()) {
            addItem(dir, true);
        }

        for (String file : filesMap.tailMap("").values()) {
            addItem(file, false);
        }

        mAdapter.notifyDataSetChanged();
        setListAdapter(mAdapter);

        //dir item, long click to select
        if (canSelectDir) {
            getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    setSelectedStateForPosition(position, true);
                    return true;
                }
            });
        }

        selectedFile = null;
        selectButton.setEnabled(false);
    }

    private void addItem(String fileName, boolean isDir) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(ITEM_NAME, fileName);
        item.put(ITEM_FILE_TYPE, isDir);
        item.put(ITEM_SELECTED, false);
        mList.add(item);
    }

    /**
     * Quando clica no item da lista, deve-se: 1) Se for diretorio, abre seus
     * arquivos filhos; 2) Se puder escolher diretorio, define-o como sendo o
     * path escolhido. 3) Se for arquivo, define-o como path escolhido. 4) Ativa
     * botao de selecao.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        File file = new File(path.get(position));

        setSelectVisible(v);

        if (file.isDirectory()) {
            if (file.canRead()) {
                lastPositions.put(currentPath, position);
                getDir(path.get(position));
            } else {
                new AlertDialog.Builder(this).setIcon(R.drawable.icon_alert)
                        .setTitle("[" + file.getName() + "] " + getText(R.string.cant_read_folder))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        } else {
            setSelectedStateForPosition(position, true);
        }
    }

    private void setSelectedStateForPosition(int position, boolean selected){
        if (selected && selectedFile != null && selectedFile.equals(path.get(position))) return;

        if (selectedFile != null){
            int index = path.indexOf(selectedFile);
            if (index != -1){
                setSelectedStateForPositionImpl(index, false);
            }
        }

        if (selected){
            setSelectedStateForPositionImpl(position, true);
            setSelectVisible(mFileName);
        }
        
        selectedFile = selected ? path.get(position) : null;
        selectButton.setEnabled(selected);
    }

    private void setSelectedStateForPositionImpl(int position, boolean selected){
        if (position >= 0 && position < mList.size()){
            Map<String,Object> item = mList.get(position);
            if (item != null) {
                item.put(ITEM_SELECTED, selected);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            selectButton.setEnabled(false);

            if (layoutCreate.getVisibility() == View.VISIBLE) {
                layoutCreate.setVisibility(View.GONE);
                layoutSelect.setVisibility(View.VISIBLE);
            } else {
                if (!currentPath.equals(ROOT)) {
                    getDir(parentPath);
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            }

            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    /**
     * Define se o botao de CREATE e visivel.
     *
     * @param v
     */
    private void setCreateVisible(View v) {
        ((TextView)findViewById(R.id.textViewFilename)).setText(mCreatingFile ? R.string.create_file : R.string.create_dir);

        layoutCreate.setVisibility(View.VISIBLE);
        layoutSelect.setVisibility(View.GONE);

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        selectButton.setEnabled(false);
    }

    /**
     * Define se o botao de SELECT e visivel.
     *
     * @param v
     */
    private void setSelectVisible(View v) {
        layoutCreate.setVisibility(View.GONE);
        layoutSelect.setVisibility(View.VISIBLE);

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        selectButton.setEnabled(false);
    }

    private boolean mCreatingFile = false;
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.fdNewDir) {
            mCreatingFile = false;
        } else if (i == R.id.fdNewFile) {
            mCreatingFile = true;
        }

        setCreateVisible(v);

        mFileName.setText("");
        mFileName.requestFocus();
    }

    @Override
    public boolean setViewValue(View view, Object data, String textRepresentation) {
        final int id = view.getId();
        if (id == R.id.fdrowimage){

            ((ImageView)view).setImageResource((boolean)data ? R.drawable.icon_folder : R.drawable.icon_file);
            return true;

        }else if (id == R.id.checkedImage){

            view.setVisibility((boolean)data ? View.VISIBLE : View.GONE);
            return true;
        }

        return false;
    }
}
