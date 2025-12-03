package pt.ipleiria.estg.dei.emergencysts.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import pt.ipleiria.estg.dei.emergencysts.modelo.Pulseira;

public class PulseiraBDHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "emergencysts.db";
    private static final int DB_VERSION = 2; // Mudámos a versão porque adicionámos colunas!

    // Nomes das colunas da tabela
    private static final String TABLE_PULSEIRA = "pulseiras";

    // Campos Básicos
    private static final String COL_ID = "id";
    private static final String COL_PRIORIDADE = "prioridade";
    private static final String COL_STATUS = "status";
    private static final String COL_NOME = "nome_paciente";
    private static final String COL_SNS = "sns";
    private static final String COL_HORA = "hora";

    // Novos Campos (Para guardar os detalhes offline)
    private static final String COL_DATA_NASC = "data_nascimento";
    private static final String COL_TELEFONE = "telefone";
    private static final String COL_MOTIVO = "motivo";
    private static final String COL_QUEIXA = "queixa";
    private static final String COL_DESCRICAO = "descricao";
    private static final String COL_INICIO = "inicio_sintomas";
    private static final String COL_DOR = "dor";
    private static final String COL_ALERGIAS = "alergias";
    private static final String COL_MEDICACAO = "medicacao";

    private static PulseiraBDHelper instance;

    private PulseiraBDHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static synchronized PulseiraBDHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PulseiraBDHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlCreate = "CREATE TABLE " + TABLE_PULSEIRA + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_PRIORIDADE + " TEXT, " +
                COL_STATUS + " TEXT, " +
                COL_NOME + " TEXT, " +
                COL_SNS + " TEXT, " +
                COL_HORA + " TEXT, " +
                COL_DATA_NASC + " TEXT, " +
                COL_TELEFONE + " TEXT, " +
                COL_MOTIVO + " TEXT, " +
                COL_QUEIXA + " TEXT, " +
                COL_DESCRICAO + " TEXT, " +
                COL_INICIO + " TEXT, " +
                COL_DOR + " TEXT, " +
                COL_ALERGIAS + " TEXT, " +
                COL_MEDICACAO + " TEXT);";
        db.execSQL(sqlCreate);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PULSEIRA);
        onCreate(db);
    }

    // --- MÉTODOS CRUD ---

    public void adicionarPulseira(Pulseira p) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_ID, p.getId());
        values.put(COL_PRIORIDADE, p.getPrioridade());
        values.put(COL_STATUS, p.getStatus());
        values.put(COL_NOME, p.getNomePaciente());
        values.put(COL_SNS, p.getSns());
        values.put(COL_HORA, p.getHora());

        // Guardar os novos campos
        values.put(COL_DATA_NASC, p.getDataNascimento());
        values.put(COL_TELEFONE, p.getTelefone());
        values.put(COL_MOTIVO, p.getMotivo());
        values.put(COL_QUEIXA, p.getQueixa());
        values.put(COL_DESCRICAO, p.getDescricao());
        values.put(COL_INICIO, p.getInicioSintomas());
        values.put(COL_DOR, p.getDor());
        values.put(COL_ALERGIAS, p.getAlergias());
        values.put(COL_MEDICACAO, p.getMedicacao());

        db.insertWithOnConflict(TABLE_PULSEIRA, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeAllPulseiras() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PULSEIRA, null, null);
    }

    public ArrayList<Pulseira> getAllPulseiras() {
        ArrayList<Pulseira> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PULSEIRA, null);

        if (cursor.moveToFirst()) {
            do {
                // Cria um objeto Pulseira com os dados do cursor
                Pulseira p = new Pulseira(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PRIORIDADE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_SNS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_HORA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DATA_NASC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TELEFONE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_MOTIVO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_QUEIXA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRICAO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_INICIO)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DOR)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ALERGIAS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_MEDICACAO))
                );
                lista.add(p);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }
}