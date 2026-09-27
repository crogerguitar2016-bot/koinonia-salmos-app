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

import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;


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


    private int atual = 0;


    private WindowManager windowManager;

    private View overlayView;

    private TextView textoProgresso;


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
            ) {

                atual =
                        nomes.size() - 1;
            }


            return !nomes.isEmpty();

        } catch (Exception e) {

            return false;
        }
    }


    private void salvarFilaCompleta() {

        try {

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

        } catch (Exception ignored) {
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

        removerOverlay();


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
                dp(9),
                dp(12),
                dp(9)
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

        titulo.setTextSize(14);

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

        textoProgresso.setTextSize(16);

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


        Button encerrar =
                new Button(this);

        encerrar.setText(
                "ENCERRAR"
        );


        botoes.addView(
                proximo
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


        int tipo;


        if (
                Build.VERSION.SDK_INT >= 26
        ) {

            tipo =
                    WindowManager
                            .LayoutParams
                            .TYPE_APPLICATION_OVERLAY;

        } else {

            tipo =
                    WindowManager
                            .LayoutParams
                            .TYPE_PHONE;
        }


        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(

                        WindowManager
                                .LayoutParams
                                .WRAP_CONTENT,

                        WindowManager
                                .LayoutParams
                                .WRAP_CONTENT,

                        tipo,

                        WindowManager
                                .LayoutParams
                                .FLAG_NOT_FOCUSABLE
                                |

                                WindowManager
                                .LayoutParams
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


        encerrar.setOnClickListener(
                v -> pausarFila()
        );
    }


    private void atualizarOverlay() {

        if (
                textoProgresso == null
                || nomes.isEmpty()
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


    private void abrirAtual() {

        if (
                atual < 0
                || atual >= nomes.size()
        ) {

            concluirFila();

            return;
        }


        atualizarOverlay();


        atualizarNotificacao();


        String mensagem =
                montarMensagem(
                        nomes.get(atual),
                        salmos.get(atual),
                        versiculos.get(atual),
                        textos.get(atual)
                );


        try {

            String url =
                    "https://wa.me/"
                            + numeros.get(atual)
                            + "?text="
                            + Uri.encode(
                            mensagem
                    );


            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );


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
                            + nomes.get(atual),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private void avancar() {

        if (
                atual + 1
                        >= nomes.size()
        ) {

            concluirFila();

            return;
        }


        atual++;


        salvarIndice();


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
                + "\n\n\n"
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


            NotificationManager nm =
                    getSystemService(
                            NotificationManager.class
                    );


            nm.createNotificationChannel(
                    canal
            );
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


        PendingIntent pending =
                PendingIntent.getService(
                        this,
                        0,
                        parar,

                        PendingIntent
                                .FLAG_UPDATE_CURRENT
                                |

                                PendingIntent
                                .FLAG_IMMUTABLE
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


        String progresso =
                nomes.isEmpty()
                        ? "Fila de mensagens"
                        : (atual + 1)
                        + "/"
                        + nomes.size();


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

                        pending
                )

                .build();
    }


    private void atualizarNotificacao() {

        NotificationManager nm =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );


        nm.notify(
                NOTIFICACAO,
                criarNotificacao()
        );
    }


    private void pausarFila() {

        salvarIndice();


        removerOverlay();


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


        removerOverlay();


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


    private void removerOverlay() {

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

        removerOverlay();

        super.onDestroy();
    }


    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}
