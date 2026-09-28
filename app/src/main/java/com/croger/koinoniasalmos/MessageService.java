package com.croger.koinoniasalmos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class MessageService extends Service {

    public static final String ACTION_START =
            "com.croger.koinoniasalmos.START";

    public static final String ACTION_RESUME =
            "com.croger.koinoniasalmos.RESUME";

    public static final String ACTION_STOP =
            "com.croger.koinoniasalmos.STOP";

    private static final String PREFS =
            "koinonia_fila";

    private static final String KEY_FILA =
            "fila_json";

    private static final String KEY_INDICE =
            "indice";

    private static final String KEY_ATIVA =
            "ativa";

    private static final String CANAL =
            "koinonia_fila";

    private static final int NOTIFICACAO =
            1201;

    private ArrayList<String> nomes =
            new ArrayList<>();

    private ArrayList<String> numeros =
            new ArrayList<>();

    private ArrayList<Integer> salmos =
            new ArrayList<>();

    private ArrayList<Integer> versiculos =
            new ArrayList<>();

    private ArrayList<String> textos =
            new ArrayList<>();

    /*
     * null = mensagem normal.
     * String, inclusive vazia =
     * mensagem personalizada para aquele contato.
     */
    private ArrayList<String> mensagensPersonalizadas =
            new ArrayList<>();

    private int atual = 0;

    private WindowManager windowManager;

    private View overlayView;

    private View overlayVersiculoView;

    private TextView textoProgresso;

    private final ArrayList<VersiculoBiblico> bancoVersiculos =
            new ArrayList<>();

    private final Random random =
            new Random();

    private int salmoSelecionado =
            -1;

    private int ultimoVersiculoSorteado =
            -1;

    private VersiculoBiblico versiculoSorteadoAtual;

    public static boolean existeFilaPendente(
            Context context
    ) {

        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                );

        return prefs.getBoolean(
                KEY_ATIVA,
                false
        )
                && prefs.getString(
                KEY_FILA,
                null
        ) != null;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        criarCanal();

        carregarBancoVersiculos();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        String acao =
                intent == null
                        ? ACTION_RESUME
                        : intent.getAction();

        if (
                ACTION_STOP.equals(
                        acao
                )
        ) {

            pausarFila();

            return START_NOT_STICKY;
        }

        boolean carregada;

        if (
                ACTION_START.equals(
                        acao
                )
        ) {

            carregada =
                    receberNovaFila(
                            intent
                    );

        } else {

            carregada =
                    carregarFilaSalva();
        }

        if (
                !carregada
        ) {

            stopSelf();

            return START_NOT_STICKY;
        }

        startForeground(
                NOTIFICACAO,
                criarNotificacao()
        );

        if (
                !podeExibirOverlay()
        ) {

            Toast.makeText(
                    this,
                    "Permita Exibir sobre outros apps.",
                    Toast.LENGTH_LONG
            ).show();

            stopForeground(
                    true
            );

            stopSelf();

            return START_NOT_STICKY;
        }

        mostrarOverlay();

        abrirAtual();

        return START_STICKY;
    }

    private boolean receberNovaFila(
            Intent intent
    ) {

        if (
                intent == null
        ) {

            return false;
        }

        nomes =
                intent.getStringArrayListExtra(
                        "nomes"
                );

        numeros =
                intent.getStringArrayListExtra(
                        "numeros"
                );

        salmos =
                intent.getIntegerArrayListExtra(
                        "salmos"
                );

        versiculos =
                intent.getIntegerArrayListExtra(
                        "versiculos"
                );

        textos =
                intent.getStringArrayListExtra(
                        "textos"
                );

        if (
                nomes == null
                        || numeros == null
                        || salmos == null
                        || versiculos == null
                        || textos == null
                        || nomes.isEmpty()
        ) {

            return false;
        }

        mensagensPersonalizadas =
                new ArrayList<>();

        for (
                int i = 0;
                i < nomes.size();
                i++
        ) {

            mensagensPersonalizadas.add(
                    null
            );
        }

        atual = 0;

        salvarFilaCompleta();

        return true;
    }

    private boolean carregarFilaSalva() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                );

        if (
                !prefs.getBoolean(
                        KEY_ATIVA,
                        false
                )
        ) {

            return false;
        }

        String salvo =
                prefs.getString(
                        KEY_FILA,
                        null
                );

        if (
                salvo == null
        ) {

            return false;
        }

        try {

            JSONObject raiz =
                    new JSONObject(
                            salvo
                    );

            JSONArray array =
                    raiz.getJSONArray(
                            "itens"
                    );

            nomes =
                    new ArrayList<>();

            numeros =
                    new ArrayList<>();

            salmos =
                    new ArrayList<>();

            versiculos =
                    new ArrayList<>();

            textos =
                    new ArrayList<>();

            mensagensPersonalizadas =
                    new ArrayList<>();

            for (
                    int i = 0;
                    i < array.length();
                    i++
            ) {

                JSONObject obj =
                        array.getJSONObject(i);

                nomes.add(
                        obj.getString(
                                "nome"
                        )
                );

                numeros.add(
                        obj.getString(
                                "numero"
                        )
                );

                salmos.add(
                        obj.getInt(
                                "salmo"
                        )
                );

                versiculos.add(
                        obj.getInt(
                                "versiculo"
                        )
                );

                textos.add(
                        obj.getString(
                                "texto"
                        )
                );

                if (
                        obj.has(
                                "mensagem_personalizada"
                        )
                                && !obj.isNull(
                                "mensagem_personalizada"
                        )
                ) {

                    mensagensPersonalizadas.add(
                            obj.getString(
                                    "mensagem_personalizada"
                            )
                    );

                } else {

                    mensagensPersonalizadas.add(
                            null
                    );
                }
            }

            atual =
                    prefs.getInt(
                            KEY_INDICE,
                            0
                    );

            if (
                    atual < 0
            ) {

                atual = 0;
            }

            if (
                    atual >= nomes.size()
                            && !nomes.isEmpty()
            ) {

                atual =
                        nomes.size() - 1;
            }

            return !nomes.isEmpty();

        } catch (Exception e) {

            return false;
        }
    }    private void garantirMensagensPersonalizadas() {

        if (
                mensagensPersonalizadas == null
        ) {

            mensagensPersonalizadas =
                    new ArrayList<>();
        }

        while (
                mensagensPersonalizadas.size()
                        < nomes.size()
        ) {

            mensagensPersonalizadas.add(
                    null
            );
        }

        while (
                mensagensPersonalizadas.size()
                        > nomes.size()
        ) {

            mensagensPersonalizadas.remove(
                    mensagensPersonalizadas.size()
                            - 1
            );
        }
    }


    private void salvarFilaCompleta() {

        try {

            garantirMensagensPersonalizadas();

            JSONObject raiz =
                    new JSONObject();

            JSONArray array =
                    new JSONArray();

            for (
                    int i = 0;
                    i < nomes.size();
                    i++
            ) {

                JSONObject obj =
                        new JSONObject();

                obj.put(
                        "nome",
                        nomes.get(i)
                );

                obj.put(
                        "numero",
                        numeros.get(i)
                );

                obj.put(
                        "salmo",
                        salmos.get(i)
                );

                obj.put(
                        "versiculo",
                        versiculos.get(i)
                );

                obj.put(
                        "texto",
                        textos.get(i)
                );

                String personalizada =
                        mensagensPersonalizadas.get(i);

                if (
                        personalizada == null
                ) {

                    obj.put(
                            "mensagem_personalizada",
                            JSONObject.NULL
                    );

                } else {

                    obj.put(
                            "mensagem_personalizada",
                            personalizada
                    );
                }

                array.put(
                        obj
                );
            }

            raiz.put(
                    "itens",
                    array
            );

            getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE
            )
                    .edit()
                    .putString(
                            KEY_FILA,
                            raiz.toString()
                    )
                    .putInt(
                            KEY_INDICE,
                            atual
                    )
                    .putBoolean(
                            KEY_ATIVA,
                            true
                    )
                    .apply();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Erro ao salvar a fila.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    private void salvarIndice() {

        getSharedPreferences(
                PREFS,
                MODE_PRIVATE
        )
                .edit()
                .putInt(
                        KEY_INDICE,
                        atual
                )
                .putBoolean(
                        KEY_ATIVA,
                        true
                )
                .apply();
    }


    private boolean podeExibirOverlay() {

        return Build.VERSION.SDK_INT < 23
                || Settings.canDrawOverlays(
                this
        );
    }


    private void mostrarOverlay() {

        removerOverlayPrincipal();

        removerPainelVersiculo();

        windowManager =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE
                        );

        LinearLayout caixa =
                new LinearLayout(this);

        caixa.setOrientation(
                LinearLayout.VERTICAL
        );

        caixa.setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
        );

        GradientDrawable fundo =
                new GradientDrawable();

        fundo.setColor(
                Color.argb(
                        235,
                        25,
                        25,
                        25
                )
        );

        fundo.setCornerRadius(
                dp(14)
        );

        caixa.setBackground(
                fundo
        );


        TextView titulo =
                new TextView(this);

        titulo.setText(
                "KOINONIA SALMOS"
        );

        titulo.setTextColor(
                Color.WHITE
        );

        titulo.setTextSize(
                14
        );

        titulo.setTypeface(
                null,
                Typeface.BOLD
        );

        titulo.setGravity(
                Gravity.CENTER
        );


        textoProgresso =
                new TextView(this);

        textoProgresso.setTextColor(
                Color.WHITE
        );

        textoProgresso.setTextSize(
                16
        );

        textoProgresso.setGravity(
                Gravity.CENTER
        );

        textoProgresso.setPadding(
                0,
                dp(4),
                0,
                dp(5)
        );


        LinearLayout botoes =
                new LinearLayout(this);

        botoes.setOrientation(
                LinearLayout.HORIZONTAL
        );


        Button proximo =
                new Button(this);

        proximo.setText(
                "PRÓXIMO"
        );


        Button botaoVersiculo =
                new Button(this);

        botaoVersiculo.setText(
                "VERSÍCULO"
        );


        Button encerrar =
                new Button(this);

        encerrar.setText(
                "ENCERRAR"
        );


        LinearLayout.LayoutParams paramsBotao =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        proximo.setLayoutParams(
                new LinearLayout.LayoutParams(
                        paramsBotao
                )
        );

        botaoVersiculo.setLayoutParams(
                new LinearLayout.LayoutParams(
                        paramsBotao
                )
        );

        encerrar.setLayoutParams(
                new LinearLayout.LayoutParams(
                        paramsBotao
                )
        );


        botoes.addView(
                proximo
        );

        botoes.addView(
                botaoVersiculo
        );

        botoes.addView(
                encerrar
        );


        caixa.addView(
                titulo
        );

        caixa.addView(
                textoProgresso
        );

        caixa.addView(
                botoes
        );


        WindowManager.LayoutParams params =
                criarParametrosOverlayPrincipal();


        overlayView =
                caixa;


        windowManager.addView(
                overlayView,
                params
        );


        atualizarOverlay();


        proximo.setOnClickListener(
                v -> avancar()
        );


        botaoVersiculo.setOnClickListener(
                v -> mostrarPainelVersiculo()
        );


        encerrar.setOnClickListener(
                v -> pausarFila()
        );
    }


    private WindowManager.LayoutParams
    criarParametrosOverlayPrincipal() {

        int tipo;

        if (
                Build.VERSION.SDK_INT >= 26
        ) {

            tipo =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            tipo =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(

                        WindowManager.LayoutParams
                                .WRAP_CONTENT,

                        WindowManager.LayoutParams
                                .WRAP_CONTENT,

                        tipo,

                        WindowManager.LayoutParams
                                .FLAG_NOT_FOCUSABLE
                                |
                                WindowManager.LayoutParams
                                        .FLAG_LAYOUT_IN_SCREEN,

                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.TOP
                        | Gravity.END;

        params.x =
                dp(8);

        params.y =
                dp(85);

        return params;
    }


    private void atualizarOverlay() {

        if (
                textoProgresso == null
                        || nomes == null
                        || nomes.isEmpty()
                        || atual < 0
                        || atual >= nomes.size()
        ) {

            return;
        }

        textoProgresso.setText(
                (atual + 1)
                        + "/"
                        + nomes.size()
                        + "  •  "
                        + nomes.get(atual)
        );
    }


    private boolean indiceAtualValido() {

        return nomes != null
                && atual >= 0
                && atual < nomes.size();
    }    private void mostrarPainelVersiculo() {

        removerOverlayPrincipal();

        removerPainelVersiculo();

        if (
                bancoVersiculos.isEmpty()
        ) {

            carregarBancoVersiculos();
        }

        if (
                bancoVersiculos.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Não foi possível carregar os Salmos.",
                    Toast.LENGTH_LONG
            ).show();

            mostrarOverlay();

            return;
        }


        windowManager =
                (WindowManager)
                        getSystemService(
                                WINDOW_SERVICE
                        );


        ScrollView scroll =
                new ScrollView(this);


        LinearLayout caixa =
                new LinearLayout(this);

        caixa.setOrientation(
                LinearLayout.VERTICAL
        );

        caixa.setPadding(
                dp(18),
                dp(14),
                dp(18),
                dp(14)
        );


        GradientDrawable fundo =
                new GradientDrawable();

        fundo.setColor(
                Color.rgb(
                        245,
                        245,
                        245
                )
        );

        fundo.setCornerRadius(
                dp(14)
        );

        caixa.setBackground(
                fundo
        );


        TextView titulo =
                new TextView(this);

        titulo.setText(
                "ESCOLHER VERSÍCULO"
        );

        titulo.setTextColor(
                Color.BLACK
        );

        titulo.setTextSize(
                18
        );

        titulo.setTypeface(
                null,
                Typeface.BOLD
        );

        titulo.setGravity(
                Gravity.CENTER
        );


        TextView instrucao =
                new TextView(this);

        instrucao.setText(
                "Digite o número do Salmo, de 1 a 150:"
        );

        instrucao.setTextColor(
                Color.DKGRAY
        );

        instrucao.setTextSize(
                15
        );

        instrucao.setPadding(
                0,
                dp(12),
                0,
                dp(4)
        );


        EditText campoSalmo =
                new EditText(this);

        campoSalmo.setHint(
                "Ex.: 23"
        );

        campoSalmo.setSingleLine(
                true
        );

        campoSalmo.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );


        Button sortear =
                new Button(this);

        sortear.setText(
                "SORTEAR VERSÍCULO"
        );


        TextView resultado =
                new TextView(this);

        resultado.setTextColor(
                Color.BLACK
        );

        resultado.setTextSize(
                17
        );

        resultado.setPadding(
                0,
                dp(12),
                0,
                dp(10)
        );


        Button outro =
                new Button(this);

        outro.setText(
                "OUTRO VERSÍCULO"
        );

        outro.setVisibility(
                View.GONE
        );


        Button usarVersiculo =
                new Button(this);

        usarVersiculo.setText(
                "USAR ESTE VERSÍCULO"
        );

        usarVersiculo.setVisibility(
                View.GONE
        );


        Button editar =
                new Button(this);

        editar.setText(
                "EDITAR MENSAGEM"
        );

        editar.setVisibility(
                View.GONE
        );


        LinearLayout areaEditor =
                new LinearLayout(this);

        areaEditor.setOrientation(
                LinearLayout.VERTICAL
        );

        areaEditor.setVisibility(
                View.GONE
        );


        TextView textoEditor =
                new TextView(this);

        textoEditor.setText(
                "Edite livremente a mensagem abaixo:"
        );

        textoEditor.setTextColor(
                Color.DKGRAY
        );

        textoEditor.setTextSize(
                14
        );

        textoEditor.setPadding(
                0,
                dp(12),
                0,
                dp(4)
        );


        EditText campoMensagem =
                new EditText(this);

        campoMensagem.setMinLines(
                6
        );

        campoMensagem.setGravity(
                Gravity.TOP
                        | Gravity.START
        );

        campoMensagem.setInputType(
                InputType.TYPE_CLASS_TEXT
                        |
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        |
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );


        Button apagarTudo =
                new Button(this);

        apagarTudo.setText(
                "APAGAR TUDO"
        );


        Button usarMensagem =
                new Button(this);

        usarMensagem.setText(
                "USAR ESTA MENSAGEM"
        );


        Button cancelarEdicao =
                new Button(this);

        cancelarEdicao.setText(
                "CANCELAR EDIÇÃO"
        );


        areaEditor.addView(
                textoEditor
        );

        areaEditor.addView(
                campoMensagem
        );

        areaEditor.addView(
                apagarTudo
        );

        areaEditor.addView(
                usarMensagem
        );

        areaEditor.addView(
                cancelarEdicao
        );


        Button mudarSalmo =
                new Button(this);

        mudarSalmo.setText(
                "MUDAR SALMO"
        );


        Button voltar =
                new Button(this);

        voltar.setText(
                "VOLTAR"
        );


        caixa.addView(
                titulo
        );

        caixa.addView(
                instrucao
        );

        caixa.addView(
                campoSalmo
        );

        caixa.addView(
                sortear
        );

        caixa.addView(
                resultado
        );

        caixa.addView(
                outro
        );

        caixa.addView(
                usarVersiculo
        );

        caixa.addView(
                editar
        );

        caixa.addView(
                areaEditor
        );

        caixa.addView(
                mudarSalmo
        );

        caixa.addView(
                voltar
        );


        scroll.addView(
                caixa
        );


        WindowManager.LayoutParams params =
                criarParametrosOverlayEditavel();


        overlayVersiculoView =
                scroll;


        windowManager.addView(
                overlayVersiculoView,
                params
        );


        sortear.setOnClickListener(
                v -> {

                    String valor =
                            campoSalmo
                                    .getText()
                                    .toString()
                                    .trim();

                    if (
                            valor.isEmpty()
                    ) {

                        Toast.makeText(
                                this,
                                "Digite o número do Salmo.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    int numeroSalmo;

                    try {

                        numeroSalmo =
                                Integer.parseInt(
                                        valor
                                );

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "Número de Salmo inválido.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    if (
                            numeroSalmo < 1
                                    || numeroSalmo > 150
                    ) {

                        Toast.makeText(
                                this,
                                "Escolha um Salmo entre 1 e 150.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    salmoSelecionado =
                            numeroSalmo;

                    ultimoVersiculoSorteado =
                            -1;


                    sortearVersiculoDoSalmo(
                            numeroSalmo,
                            resultado,
                            outro,
                            usarVersiculo,
                            editar
                    );


                    esconderTeclado(
                            campoSalmo
                    );
                }
        );


        outro.setOnClickListener(
                v -> {

                    if (
                            salmoSelecionado < 1
                                    || salmoSelecionado > 150
                    ) {

                        Toast.makeText(
                                this,
                                "Escolha primeiro um Salmo.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    areaEditor.setVisibility(
                            View.GONE
                    );


                    esconderTeclado(
                            campoMensagem
                    );


                    sortearVersiculoDoSalmo(
                            salmoSelecionado,
                            resultado,
                            outro,
                            usarVersiculo,
                            editar
                    );
                }
        );


        usarVersiculo.setOnClickListener(
                v -> usarVersiculoSelecionado()
        );


        editar.setOnClickListener(
                v -> {

                    if (
                            versiculoSorteadoAtual == null
                    ) {

                        Toast.makeText(
                                this,
                                "Sorteie um versículo primeiro.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    campoMensagem.setText(
                            montarMensagem(
                                    nomes.get(
                                            atual
                                    ),
                                    versiculoSorteadoAtual.salmo,
                                    versiculoSorteadoAtual.versiculo,
                                    versiculoSorteadoAtual.texto
                            )
                    );


                    campoMensagem.setSelection(
                            campoMensagem
                                    .getText()
                                    .length()
                    );


                    areaEditor.setVisibility(
                            View.VISIBLE
                    );


                    campoMensagem.requestFocus();


                    InputMethodManager teclado =
                            (InputMethodManager)
                                    getSystemService(
                                            INPUT_METHOD_SERVICE
                                    );


                    if (
                            teclado != null
                    ) {

                        teclado.showSoftInput(
                                campoMensagem,
                                InputMethodManager
                                        .SHOW_IMPLICIT
                        );
                    }
                }
        );


        apagarTudo.setOnClickListener(
                v -> {

                    campoMensagem.setText(
                            ""
                    );

                    campoMensagem.requestFocus();
                }
        );


        usarMensagem.setOnClickListener(
                v -> usarMensagemPersonalizada(
                        campoMensagem
                )
        );


        cancelarEdicao.setOnClickListener(
                v -> {

                    esconderTeclado(
                            campoMensagem
                    );

                    areaEditor.setVisibility(
                            View.GONE
                    );
                }
        );


        mudarSalmo.setOnClickListener(
                v -> {

                    salmoSelecionado =
                            -1;

                    versiculoSorteadoAtual =
                            null;

                    ultimoVersiculoSorteado =
                            -1;


                    campoSalmo.setText(
                            ""
                    );

                    resultado.setText(
                            ""
                    );

                    outro.setVisibility(
                            View.GONE
                    );

                    usarVersiculo.setVisibility(
                            View.GONE
                    );

                    editar.setVisibility(
                            View.GONE
                    );

                    areaEditor.setVisibility(
                            View.GONE
                    );


                    campoSalmo.requestFocus();


                    InputMethodManager teclado =
                            (InputMethodManager)
                                    getSystemService(
                                            INPUT_METHOD_SERVICE
                                    );


                    if (
                            teclado != null
                    ) {

                        teclado.showSoftInput(
                                campoSalmo,
                                InputMethodManager
                                        .SHOW_IMPLICIT
                        );
                    }
                }
        );


        voltar.setOnClickListener(
                v -> mostrarOverlay()
        );
    }    private WindowManager.LayoutParams
    criarParametrosOverlayEditavel() {

        int tipo;

        if (
                Build.VERSION.SDK_INT >= 26
        ) {

            tipo =
                    WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            tipo =
                    WindowManager.LayoutParams
                            .TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(

                        WindowManager.LayoutParams
                                .MATCH_PARENT,

                        WindowManager.LayoutParams
                                .WRAP_CONTENT,

                        tipo,

                        WindowManager.LayoutParams
                                .FLAG_LAYOUT_IN_SCREEN,

                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.TOP
                        | Gravity.CENTER_HORIZONTAL;

        params.y =
                dp(60);

        params.softInputMode =
                WindowManager.LayoutParams
                        .SOFT_INPUT_ADJUST_RESIZE;

        return params;
    }


    private void carregarBancoVersiculos() {

        bancoVersiculos.clear();

        try {

            InputStream input =
                    getAssets()
                            .open(
                                    "salmos_blivre.json"
                            );

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder json =
                    new StringBuilder();

            String linha;

            while (
                    (linha =
                            reader.readLine())
                            != null
            ) {

                json.append(
                        linha
                );
            }

            reader.close();

            input.close();


            JSONObject raiz =
                    new JSONObject(
                            json.toString()
                    );

            JSONArray array =
                    raiz.getJSONArray(
                            "versiculos"
                    );


            for (
                    int i = 0;
                    i < array.length();
                    i++
            ) {

                JSONObject item =
                        array.getJSONObject(
                                i
                        );

                bancoVersiculos.add(
                        new VersiculoBiblico(

                                item.getInt(
                                        "salmo"
                                ),

                                item.getInt(
                                        "versiculo"
                                ),

                                item.getString(
                                        "texto"
                                )
                        )
                );
            }

        } catch (Exception e) {

            bancoVersiculos.clear();

            Toast.makeText(
                    this,
                    "Erro ao carregar salmos_blivre.json.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private ArrayList<VersiculoBiblico>
    obterVersiculosDoSalmo(
            int numeroSalmo
    ) {

        ArrayList<VersiculoBiblico> lista =
                new ArrayList<>();

        for (
                VersiculoBiblico item :
                        bancoVersiculos
        ) {

            if (
                    item.salmo
                            == numeroSalmo
            ) {

                lista.add(
                        item
                );
            }
        }

        return lista;
    }


    private void sortearVersiculoDoSalmo(
            int numeroSalmo,
            TextView resultado,
            Button outro,
            Button usarVersiculo,
            Button editar
    ) {

        ArrayList<VersiculoBiblico> lista =
                obterVersiculosDoSalmo(
                        numeroSalmo
                );

        if (
                lista.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Nenhum versículo encontrado no Salmo "
                            + numeroSalmo
                            + ".",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        VersiculoBiblico escolhido;


        if (
                lista.size() == 1
        ) {

            escolhido =
                    lista.get(
                            0
                    );

        } else {

            do {

                escolhido =
                        lista.get(
                                random.nextInt(
                                        lista.size()
                                )
                        );

            } while (
                    escolhido.versiculo
                            == ultimoVersiculoSorteado
            );
        }


        versiculoSorteadoAtual =
                escolhido;

        ultimoVersiculoSorteado =
                escolhido.versiculo;


        resultado.setText(
                "Salmo "
                        + escolhido.salmo
                        + ":"
                        + escolhido.versiculo
                        + "\n\n"
                        + escolhido.texto
                        + "\n\n(BLIVRE)"
        );


        outro.setVisibility(
                View.VISIBLE
        );

        usarVersiculo.setVisibility(
                View.VISIBLE
        );

        editar.setVisibility(
                View.VISIBLE
        );
    }


    private void usarVersiculoSelecionado() {

        if (
                !indiceAtualValido()
                        || versiculoSorteadoAtual == null
        ) {

            Toast.makeText(
                    this,
                    "Sorteie um versículo primeiro.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        salmos.set(
                atual,
                versiculoSorteadoAtual.salmo
        );

        versiculos.set(
                atual,
                versiculoSorteadoAtual.versiculo
        );

        textos.set(
                atual,
                versiculoSorteadoAtual.texto
        );


        garantirMensagensPersonalizadas();


        mensagensPersonalizadas.set(
                atual,
                null
        );


        salvarFilaCompleta();


        mostrarOverlay();


        abrirAtual();
    }


    private void usarMensagemPersonalizada(
            EditText campoMensagem
    ) {

        if (
                !indiceAtualValido()
                        || campoMensagem == null
        ) {

            return;
        }


        if (
                versiculoSorteadoAtual != null
        ) {

            salmos.set(
                    atual,
                    versiculoSorteadoAtual.salmo
            );

            versiculos.set(
                    atual,
                    versiculoSorteadoAtual.versiculo
            );

            textos.set(
                    atual,
                    versiculoSorteadoAtual.texto
            );
        }


        garantirMensagensPersonalizadas();


        /*
         * Não usamos trim().
         * Se o usuário apagar tudo,
         * a mensagem vazia será mantida.
         */
        mensagensPersonalizadas.set(
                atual,
                campoMensagem
                        .getText()
                        .toString()
        );


        salvarFilaCompleta();


        esconderTeclado(
                campoMensagem
        );


        mostrarOverlay();


        abrirAtual();
    }


    private void esconderTeclado(
            View view
    ) {

        if (
                view == null
        ) {

            return;
        }


        InputMethodManager teclado =
                (InputMethodManager)
                        getSystemService(
                                INPUT_METHOD_SERVICE
                        );


        if (
                teclado != null
        ) {

            teclado.hideSoftInputFromWindow(
                    view.getWindowToken(),
                    0
            );
        }
    }    private void abrirAtual() {

        if (
                !indiceAtualValido()
        ) {

            concluirFila();

            return;
        }


        atualizarOverlay();

        atualizarNotificacao();

        garantirMensagensPersonalizadas();


        String personalizada =
                mensagensPersonalizadas.get(
                        atual
                );


        String mensagem;


        if (
                personalizada != null
        ) {

            /*
             * Usa exatamente o texto que foi editado.
             * Inclusive uma String vazia é válida.
             */
            mensagem =
                    personalizada;

        } else {

            mensagem =
                    montarMensagem(
                            nomes.get(
                                    atual
                            ),
                            salmos.get(
                                    atual
                            ),
                            versiculos.get(
                                    atual
                            ),
                            textos.get(
                                    atual
                            )
                    );
        }


        try {

            String url =
                    "https://wa.me/"
                            + numeros.get(
                            atual
                    );


            /*
             * Se a mensagem estiver vazia,
             * abre o contato sem texto preenchido.
             */
            if (
                    mensagem != null
                            && !mensagem.isEmpty()
            ) {

                url =
                        url
                                + "?text="
                                + Uri.encode(
                                mensagem
                        );
            }


            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    url
                            )
                    );


            /*
             * WhatsApp Business.
             */
            intent.setPackage(
                    "com.whatsapp.w4b"
            );


            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );


            startActivity(
                    intent
            );


        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Não foi possível abrir "
                            + nomes.get(
                            atual
                    )
                            + " no WhatsApp Business.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private void avancar() {

        /*
         * Ao tocar PRÓXIMO, qualquer tela
         * de versículo ou edição é encerrada.
         */
        removerPainelVersiculo();


        if (
                atual + 1
                        >= nomes.size()
        ) {

            concluirFila();

            return;
        }


        atual++;


        salvarIndice();


        /*
         * O novo contato sempre começa novamente
         * somente com:
         *
         * PRÓXIMO | VERSÍCULO | ENCERRAR
         */
        mostrarOverlay();


        abrirAtual();
    }


    private String montarMensagem(
            String nome,
            int salmo,
            int versiculo,
            String texto
    ) {

        return nome
                + ", "
                + saudacao()
                + "! "
                + "Um versículo especial para você."
                + "\n\n"
                + "Salmo "
                + salmo
                + ":"
                + versiculo
                + " — "
                + texto
                + " "
                + "(BLIVRE)";
    }


    private String saudacao() {

        int hora =
                Calendar
                        .getInstance()
                        .get(
                                Calendar.HOUR_OF_DAY
                        );


        if (
                hora >= 5
                        && hora < 12
        ) {

            return "bom dia";
        }


        if (
                hora >= 12
                        && hora < 18
        ) {

            return "boa tarde";
        }


        return "boa noite";
    }


    private void criarCanal() {

        if (
                Build.VERSION.SDK_INT >= 26
        ) {

            NotificationChannel canal =
                    new NotificationChannel(
                            CANAL,
                            "Fila Koinonia",
                            NotificationManager
                                    .IMPORTANCE_LOW
                    );


            canal.setDescription(
                    "Fila de mensagens Koinonia Salmos"
            );


            NotificationManager gerenciador =
                    getSystemService(
                            NotificationManager.class
                    );


            if (
                    gerenciador != null
            ) {

                gerenciador.createNotificationChannel(
                        canal
                );
            }
        }
    }


    private Notification criarNotificacao() {

        Intent parar =
                new Intent(
                        this,
                        MessageService.class
                );


        parar.setAction(
                ACTION_STOP
        );


        PendingIntent pendingParar =
                PendingIntent.getService(
                        this,
                        0,
                        parar,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                |
                                PendingIntent.FLAG_IMMUTABLE
                );


        Notification.Builder builder;


        if (
                Build.VERSION.SDK_INT >= 26
        ) {

            builder =
                    new Notification.Builder(
                            this,
                            CANAL
                    );

        } else {

            builder =
                    new Notification.Builder(
                            this
                    );
        }


        String progresso;


        if (
                nomes == null
                        || nomes.isEmpty()
        ) {

            progresso =
                    "Fila de mensagens";

        } else {

            progresso =
                    (atual + 1)
                            + "/"
                            + nomes.size();
        }


        return builder

                .setSmallIcon(
                        android.R.drawable
                                .ic_dialog_info
                )

                .setContentTitle(
                        "Koinonia Salmos"
                )

                .setContentText(
                        progresso
                )

                .setOngoing(
                        true
                )

                .addAction(
                        android.R.drawable
                                .ic_menu_close_clear_cancel,
                        "Pausar",
                        pendingParar
                )

                .build();
    }


    private void atualizarNotificacao() {

        NotificationManager gerenciador =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );


        if (
                gerenciador != null
        ) {

            gerenciador.notify(
                    NOTIFICACAO,
                    criarNotificacao()
            );
        }
    }    private void pausarFila() {

        /*
         * Preserva exatamente o ponto atual da fila.
         * Ao abrir novamente o aplicativo,
         * será possível continuar deste contato.
         */
        salvarFilaCompleta();


        removerPainelVersiculo();

        removerOverlayPrincipal();


        stopForeground(
                true
        );


        stopSelf();


        Toast.makeText(
                this,
                "Fila pausada. Você poderá continuar depois.",
                Toast.LENGTH_LONG
        ).show();
    }


    private void concluirFila() {

        /*
         * A fila chegou ao último contato.
         * Agora ela deixa de ser considerada pendente.
         */
        getSharedPreferences(
                PREFS,
                MODE_PRIVATE
        )
                .edit()
                .putBoolean(
                        KEY_ATIVA,
                        false
                )
                .remove(
                        KEY_FILA
                )
                .remove(
                        KEY_INDICE
                )
                .apply();


        removerPainelVersiculo();

        removerOverlayPrincipal();


        stopForeground(
                true
        );


        stopSelf();


        Toast.makeText(
                this,
                "Fila concluída.",
                Toast.LENGTH_LONG
        ).show();
    }


    private void removerOverlayPrincipal() {

        if (
                overlayView != null
                        && windowManager != null
        ) {

            try {

                windowManager.removeView(
                        overlayView
                );

            } catch (Exception ignored) {

            }
        }


        overlayView =
                null;


        textoProgresso =
                null;
    }


    private void removerPainelVersiculo() {

        if (
                overlayVersiculoView != null
                        && windowManager != null
        ) {

            try {

                windowManager.removeView(
                        overlayVersiculoView
                );

            } catch (Exception ignored) {

            }
        }


        overlayVersiculoView =
                null;


        /*
         * O estado temporário também é zerado.
         *
         * Por isso, ao passar para o próximo contato,
         * não permanece na tela o Salmo ou a mensagem
         * do contato anterior.
         */
        salmoSelecionado =
                -1;


        ultimoVersiculoSorteado =
                -1;


        versiculoSorteadoAtual =
                null;
    }


    private int dp(
            int valor
    ) {

        float densidade =
                getResources()
                        .getDisplayMetrics()
                        .density;


        return (int) (
                valor * densidade
        );
    }


    @Override
    public void onDestroy() {

        /*
         * Remove qualquer janela flutuante que ainda
         * esteja na tela quando o serviço for encerrado.
         */
        removerPainelVersiculo();

        removerOverlayPrincipal();


        super.onDestroy();
    }


    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }    private static class VersiculoBiblico {

        final int salmo;

        final int versiculo;

        final String texto;


        VersiculoBiblico(
                int salmo,
                int versiculo,
                String texto
        ) {

            this.salmo =
                    salmo;

            this.versiculo =
                    versiculo;

            this.texto =
                    texto;
        }
    }
}
