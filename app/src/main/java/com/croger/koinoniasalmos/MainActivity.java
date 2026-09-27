package com.croger.koinoniasalmos;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class MainActivity extends Activity {

    private static final int PEDIDO_CONTATOS = 1001;

    private static final String PREFS =
            "koinonia_salmos";

    private static final String ULTIMA_DISTRIBUICAO =
            "ultima_distribuicao";

    private static final int ACAO_NENHUMA = 0;
    private static final int ACAO_NOVA_FILA = 1;
    private static final int ACAO_CONTINUAR = 2;


    private final ArrayList<Contato> contatos =
            new ArrayList<>();

    private final ArrayList<Versiculo> versiculos =
            new ArrayList<>();


    private LinearLayout areaContatos;

    private TextView statusContatos;
    private TextView statusSelecionados;
    private TextView statusSalmo;

    private Spinner spinnerSalmo;

    private Button botaoAtualizar;
    private Button botaoTodos;
    private Button botaoNenhum;
    private Button botaoIniciar;
    private Button botaoContinuar;
    private Button botaoRepetir;


    private ArrayList<String> pendenteNomes;
    private ArrayList<String> pendenteNumeros;

    private ArrayList<Integer> pendenteSalmos;
    private ArrayList<Integer> pendenteVersiculos;

    private ArrayList<String> pendenteTextos;


    private int acaoDepoisOverlay =
            ACAO_NENHUMA;


    static class Contato {

        String nome;
        String nomeCompleto;
        String numero;

        CheckBox checkBox;

        Contato(
                String nome,
                String nomeCompleto,
                String numero
        ) {
            this.nome = nome;
            this.nomeCompleto = nomeCompleto;
            this.numero = numero;
        }
    }


    static class ContatoAgrupado {

        long contactId;

        String nomeCompleto;
        String numero;

        int prioridade;

        ContatoAgrupado(
                long contactId,
                String nomeCompleto,
                String numero,
                int prioridade
        ) {

            this.contactId = contactId;
            this.nomeCompleto = nomeCompleto;
            this.numero = numero;
            this.prioridade = prioridade;
        }
    }


    static class Versiculo {

        int salmo;
        int versiculo;

        String texto;

        Versiculo(
                int salmo,
                int versiculo,
                String texto
        ) {

            this.salmo = salmo;
            this.versiculo = versiculo;
            this.texto = texto;
        }
    }


    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        criarInterface();

        carregarSalmos();

        verificarPermissaoContatos();
    }


    private void criarInterface() {

        int margem = dp(18);

        LinearLayout raiz =
                new LinearLayout(this);

        raiz.setOrientation(
                LinearLayout.VERTICAL
        );

        raiz.setPadding(
                margem,
                margem,
                margem,
                margem
        );


        TextView titulo =
                new TextView(this);

        titulo.setText(
                "KOINONIA — SALMOS"
        );

        titulo.setTextSize(27);

        titulo.setTypeface(
                null,
                Typeface.BOLD
        );

        titulo.setGravity(
                Gravity.CENTER
        );


        TextView subtitulo =
                new TextView(this);

        subtitulo.setText(
                "\nEnvio assistido de versículos\n"
                        + "Você controla quando passar para a próxima pessoa.\n"
        );

        subtitulo.setTextSize(17);

        subtitulo.setGravity(
                Gravity.CENTER
        );


        statusContatos =
                new TextView(this);

        statusContatos.setText(
                "Procurando contatos Koinonia..."
        );

        statusContatos.setTextSize(17);

        statusContatos.setGravity(
                Gravity.CENTER
        );


        botaoAtualizar =
                new Button(this);

        botaoAtualizar.setText(
                "ATUALIZAR CONTATOS"
        );


        LinearLayout linhaBotoes =
                new LinearLayout(this);

        linhaBotoes.setOrientation(
                LinearLayout.HORIZONTAL
        );


        botaoTodos =
                new Button(this);

        botaoTodos.setText(
                "SELECIONAR TODOS"
        );


        botaoNenhum =
                new Button(this);

        botaoNenhum.setText(
                "LIMPAR"
        );


        LinearLayout.LayoutParams metade =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );


        linhaBotoes.addView(
                botaoTodos,
                metade
        );

        linhaBotoes.addView(
                botaoNenhum,
                metade
        );


        statusSelecionados =
                new TextView(this);

        statusSelecionados.setTextSize(17);

        statusSelecionados.setGravity(
                Gravity.CENTER
        );


        areaContatos =
                new LinearLayout(this);

        areaContatos.setOrientation(
                LinearLayout.VERTICAL
        );

        areaContatos.setPadding(
                0,
                dp(8),
                0,
                dp(12)
        );


        TextView tituloSalmo =
                new TextView(this);

        tituloSalmo.setText(
                "\nESCOLHA O SALMO"
        );

        tituloSalmo.setTextSize(20);

        tituloSalmo.setTypeface(
                null,
                Typeface.BOLD
        );

        tituloSalmo.setGravity(
                Gravity.CENTER
        );


        spinnerSalmo =
                new Spinner(this);


        ArrayList<String> opcoes =
                new ArrayList<>();

        for (
                int i = 1;
                i <= 150;
                i++
        ) {

            opcoes.add(
                    "Salmo " + i
            );
        }


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_spinner_dropdown_item,
                        opcoes
                );


        spinnerSalmo.setAdapter(
                adapter
        );


        statusSalmo =
                new TextView(this);

        statusSalmo.setTextSize(17);

        statusSalmo.setGravity(
                Gravity.CENTER
        );


        botaoIniciar =
                new Button(this);

        botaoIniciar.setText(
                "INICIAR NOVA FILA"
        );

        botaoIniciar.setEnabled(
                false
        );


        botaoContinuar =
                new Button(this);

        botaoContinuar.setText(
                "CONTINUAR FILA ANTERIOR"
        );

        botaoContinuar.setEnabled(
                false
        );


        botaoRepetir =
                new Button(this);

        botaoRepetir.setText(
                "REPETIR SELECIONADOS\n"
                        + "COM OS MESMOS VERSÍCULOS"
        );

        botaoRepetir.setEnabled(
                false
        );


        raiz.addView(titulo);
        raiz.addView(subtitulo);
        raiz.addView(statusContatos);
        raiz.addView(botaoAtualizar);
        raiz.addView(linhaBotoes);
        raiz.addView(statusSelecionados);
        raiz.addView(areaContatos);
        raiz.addView(tituloSalmo);
        raiz.addView(spinnerSalmo);
        raiz.addView(statusSalmo);
        raiz.addView(botaoIniciar);
        raiz.addView(botaoContinuar);
        raiz.addView(botaoRepetir);


        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(
                raiz,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );


        setContentView(
                scroll
        );


        botaoAtualizar.setOnClickListener(
                v -> carregarContatos()
        );


        botaoTodos.setOnClickListener(
                v -> selecionarTodos(true)
        );


        botaoNenhum.setOnClickListener(
                v -> selecionarTodos(false)
        );


        spinnerSalmo.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        atualizarStatusSalmo();
                    }


                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );


        botaoIniciar.setOnClickListener(
                v -> prepararNovaRodada()
        );


        botaoContinuar.setOnClickListener(
                v -> continuarFila()
        );


        botaoRepetir.setOnClickListener(
                v -> prepararRepeticao()
        );
    }

    private void carregarSalmos() {

        versiculos.clear();

        try {

            InputStream input =
                    getAssets().open(
                            "salmos_blivre.json"
                    );


            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    );


            StringBuilder sb =
                    new StringBuilder();


            String linha;

            while (
                    (linha = reader.readLine())
                            != null
            ) {

                sb.append(linha);
            }


            reader.close();


            JSONObject raiz =
                    new JSONObject(
                            sb.toString()
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

                JSONObject obj =
                        array.getJSONObject(i);


                versiculos.add(
                        new Versiculo(
                                obj.getInt("salmo"),
                                obj.getInt("versiculo"),
                                obj.getString("texto")
                        )
                );
            }


            atualizarStatusSalmo();

        } catch (Exception e) {

            statusSalmo.setText(
                    "Erro ao carregar Salmos."
            );


            Toast.makeText(
                    this,
                    "Erro ao carregar Salmos: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private ArrayList<Versiculo>
    versiculosDoSalmo(
            int numero
    ) {

        ArrayList<Versiculo> resultado =
                new ArrayList<>();


        for (Versiculo v : versiculos) {

            if (v.salmo == numero) {

                resultado.add(v);
            }
        }


        return resultado;
    }


    private void atualizarStatusSalmo() {

        if (
                spinnerSalmo == null
                || versiculos.isEmpty()
        ) {

            return;
        }


        int numero =
                spinnerSalmo
                        .getSelectedItemPosition()
                        + 1;


        int quantidade =
                versiculosDoSalmo(
                        numero
                ).size();


        statusSalmo.setText(
                "Salmo "
                        + numero
                        + " — "
                        + quantidade
                        + " versículo(s)\n"
        );
    }


    private void verificarPermissaoContatos() {

        if (
                Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                Manifest.permission.READ_CONTACTS
        )
                != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_CONTACTS
                    },
                    PEDIDO_CONTATOS
            );

            return;
        }


        carregarContatos();
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );


        if (
                requestCode
                        == PEDIDO_CONTATOS
        ) {

            if (
                    grantResults.length > 0
                    && grantResults[0]
                    == PackageManager
                    .PERMISSION_GRANTED
            ) {

                carregarContatos();

            } else {

                statusContatos.setText(
                        "Permissão para contatos necessária."
                );
            }
        }
    }


    private void carregarContatos() {

        if (
                Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                Manifest.permission.READ_CONTACTS
        )
                != PackageManager.PERMISSION_GRANTED
        ) {

            verificarPermissaoContatos();

            return;
        }


        contatos.clear();

        areaContatos.removeAllViews();


        /*
         * A chave agora é CONTACT_ID.
         *
         * Portanto, um contato com dois números
         * continua aparecendo apenas UMA VEZ.
         */
        LinkedHashMap<Long, ContatoAgrupado> agrupados =
                new LinkedHashMap<>();


        String[] colunas = {

                ContactsContract
                        .CommonDataKinds
                        .Phone
                        .CONTACT_ID,

                ContactsContract
                        .CommonDataKinds
                        .Phone
                        .DISPLAY_NAME,

                ContactsContract
                        .CommonDataKinds
                        .Phone
                        .NUMBER,

                ContactsContract
                        .CommonDataKinds
                        .Phone
                        .IS_PRIMARY
        };


        Cursor cursor =
                getContentResolver().query(

                        ContactsContract
                                .CommonDataKinds
                                .Phone
                                .CONTENT_URI,

                        colunas,

                        null,
                        null,
                        null
                );


        if (cursor != null) {

            int idxId =
                    cursor.getColumnIndex(
                            ContactsContract
                                    .CommonDataKinds
                                    .Phone
                                    .CONTACT_ID
                    );


            int idxNome =
                    cursor.getColumnIndex(
                            ContactsContract
                                    .CommonDataKinds
                                    .Phone
                                    .DISPLAY_NAME
                    );


            int idxNumero =
                    cursor.getColumnIndex(
                            ContactsContract
                                    .CommonDataKinds
                                    .Phone
                                    .NUMBER
                    );


            int idxPrincipal =
                    cursor.getColumnIndex(
                            ContactsContract
                                    .CommonDataKinds
                                    .Phone
                                    .IS_PRIMARY
                    );


            while (
                    cursor.moveToNext()
            ) {

                if (
                        idxId < 0
                        || idxNome < 0
                        || idxNumero < 0
                ) {

                    break;
                }


                long contactId =
                        cursor.getLong(
                                idxId
                        );


                String nomeCompleto =
                        cursor.getString(
                                idxNome
                        );


                String numero =
                        cursor.getString(
                                idxNumero
                        );


                int principal = 0;


                if (
                        idxPrincipal >= 0
                        && !cursor.isNull(
                        idxPrincipal
                )
                ) {

                    principal =
                            cursor.getInt(
                                    idxPrincipal
                            );
                }


                if (
                        nomeCompleto == null
                        || numero == null
                ) {

                    continue;
                }


                if (
                        !ehContatoKoinonia(
                                nomeCompleto
                        )
                ) {

                    continue;
                }


                numero =
                        normalizarNumero(
                                numero
                        );


                if (numero == null) {
                    continue;
                }


                ContatoAgrupado anterior =
                        agrupados.get(
                                contactId
                        );


                if (
                        anterior == null
                        || principal
                        > anterior.prioridade
                ) {

                    agrupados.put(
                            contactId,

                            new ContatoAgrupado(
                                    contactId,
                                    nomeCompleto,
                                    numero,
                                    principal
                            )
                    );
                }
            }


            cursor.close();
        }


        Set<String> numerosUsados =
                new HashSet<>();


        for (
                ContatoAgrupado item :
                agrupados.values()
        ) {

            if (
                    numerosUsados.contains(
                            item.numero
                    )
            ) {

                continue;
            }


            String primeiroNome =
                    limparPrimeiroNome(
                            item.nomeCompleto
                    );


            if (
                    primeiroNome.isEmpty()
            ) {

                continue;
            }


            numerosUsados.add(
                    item.numero
            );


            contatos.add(
                    new Contato(
                            primeiroNome,
                            item.nomeCompleto,
                            item.numero
                    )
            );
        }


        Collections.sort(
                contatos,
                (a, b) ->
                        a.nome.compareToIgnoreCase(
                                b.nome
                        )
        );


        for (
                Contato contato :
                contatos
        ) {

            CheckBox cb =
                    new CheckBox(this);


            cb.setText(
                    contato.nome
            );


            cb.setTextSize(18);


            cb.setChecked(
                    true
            );


            cb.setOnCheckedChangeListener(
                    (buttonView, isChecked) ->
                            atualizarSelecionados()
            );


            contato.checkBox =
                    cb;


            areaContatos.addView(
                    cb
            );
        }


        statusContatos.setText(
                "CONTATOS KOINONIA ENCONTRADOS: "
                        + contatos.size()
        );


        atualizarSelecionados();


        botaoIniciar.setEnabled(
                !contatos.isEmpty()
                        && !versiculos.isEmpty()
        );


        botaoRepetir.setEnabled(
                existeUltimaDistribuicao()
        );


        botaoContinuar.setEnabled(
                MessageService
                        .existeFilaPendente(
                                this
                        )
        );
    }


    private boolean ehContatoKoinonia(
            String nome
    ) {

        String n =
                nome.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );


        return n.equals("koinonia")
                || n.endsWith(" koinonia")

                || n.equals("coinonia")
                || n.endsWith(" coinonia")

                || n.equals("quaerinonia")
                || n.endsWith(" quaerinonia");
    }


    private String limparPrimeiroNome(
            String nomeCompleto
    ) {

        String nome =
                nomeCompleto.replaceFirst(
                        "(?i)\\s+"
                                + "(Koinonia|Coinonia|Quaerinonia)"
                                + "\\s*$",
                        ""
                ).trim();


        if (nome.isEmpty()) {
            return "";
        }


        String primeiro =
                nome.split("\\s+")[0];


        primeiro =
                primeiro.replaceFirst(
                        "^[^\\p{L}\\p{N}]+",
                        ""
                );


        primeiro =
                primeiro.replaceFirst(
                        "[^\\p{L}\\p{N}]+$",
                        ""
                );


        if (primeiro.isEmpty()) {
            return "";
        }


        return primeiro.substring(
                0,
                1
        ).toUpperCase(
                Locale.getDefault()
        )
                + primeiro.substring(1);
    }


    private String normalizarNumero(
            String numero
    ) {

        numero =
                numero.replaceAll(
                        "\\D",
                        ""
                );


        if (numero.isEmpty()) {
            return null;
        }


        if (
                numero.startsWith("00")
        ) {

            numero =
                    numero.substring(2);
        }


        if (
                numero.length() == 10
                || numero.length() == 11
        ) {

            numero =
                    "55" + numero;
        }


        if (
                numero.length() < 10
        ) {

            return null;
        }


        return numero;
    }

    private void selecionarTodos(
            boolean selecionar
    ) {

        for (
                Contato contato :
                contatos
        ) {

            if (
                    contato.checkBox
                            != null
            ) {

                contato.checkBox
                        .setChecked(
                                selecionar
                        );
            }
        }


        atualizarSelecionados();
    }


    private ArrayList<Contato>
    contatosSelecionados() {

        ArrayList<Contato> resultado =
                new ArrayList<>();


        for (
                Contato contato :
                contatos
        ) {

            if (
                    contato.checkBox != null
                    && contato
                    .checkBox
                    .isChecked()
            ) {

                resultado.add(
                        contato
                );
            }
        }


        return resultado;
    }


    private void atualizarSelecionados() {

        int quantidade =
                contatosSelecionados()
                        .size();


        statusSelecionados.setText(
                "\nSelecionados: "
                        + quantidade
                        + " de "
                        + contatos.size()
                        + "\n"
        );
    }


    private void prepararNovaRodada() {

        ArrayList<Contato> selecionados =
                contatosSelecionados();


        if (
                selecionados.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Selecione pelo menos uma pessoa.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        int numeroSalmo =
                spinnerSalmo
                        .getSelectedItemPosition()
                        + 1;


        ArrayList<Versiculo> capitulo =
                versiculosDoSalmo(
                        numeroSalmo
                );


        if (
                capitulo.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Esse Salmo não possui versículos carregados.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        Collections.shuffle(
                capitulo
        );


        LinkedHashMap<String, Versiculo> distribuicao =
                new LinkedHashMap<>();


        for (
                int i = 0;
                i < selecionados.size();
                i++
        ) {

            Contato contato =
                    selecionados.get(i);


            Versiculo versiculo =
                    capitulo.get(
                            i % capitulo.size()
                    );


            distribuicao.put(
                    contato.numero,
                    versiculo
            );
        }


        salvarUltimaDistribuicao(
                distribuicao
        );


        mostrarConfirmacao(
                selecionados,
                distribuicao,
                false
        );
    }


    private void prepararRepeticao() {

        ArrayList<Contato> selecionados =
                contatosSelecionados();


        if (
                selecionados.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Selecione quem deseja repetir.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        Map<String, Versiculo> anterior =
                carregarUltimaDistribuicao();


        LinkedHashMap<String, Versiculo> repetir =
                new LinkedHashMap<>();


        ArrayList<Contato> validos =
                new ArrayList<>();


        for (
                Contato contato :
                selecionados
        ) {

            Versiculo v =
                    anterior.get(
                            contato.numero
                    );


            if (
                    v != null
            ) {

                repetir.put(
                        contato.numero,
                        v
                );


                validos.add(
                        contato
                );
            }
        }


        if (
                validos.isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Nenhum selecionado pertence à última distribuição.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        mostrarConfirmacao(
                validos,
                repetir,
                true
        );
    }


    private void mostrarConfirmacao(
            ArrayList<Contato> selecionados,
            Map<String, Versiculo> distribuicao,
            boolean repeticao
    ) {

        StringBuilder texto =
                new StringBuilder();


        if (repeticao) {

            texto.append(
                    "MESMOS VERSÍCULOS\n\n"
            );

        } else {

            texto.append(
                    "DISTRIBUIÇÃO\n\n"
            );
        }


        for (
                Contato contato :
                selecionados
        ) {

            Versiculo v =
                    distribuicao.get(
                            contato.numero
                    );


            if (
                    v == null
            ) {

                continue;
            }


            texto.append(
                    contato.nome
            );

            texto.append(
                    " → Salmo "
            );

            texto.append(
                    v.salmo
            );

            texto.append(
                    ":"
            );

            texto.append(
                    v.versiculo
            );

            texto.append(
                    "\n"
            );
        }


        texto.append(
                "\nSem temporizador automático."
        );


        texto.append(
                "\n\nDepois de enviar no WhatsApp, "
                        + "toque em PRÓXIMO."
        );


        new AlertDialog.Builder(this)

                .setTitle(
                        repeticao
                                ? "Repetir mensagens"
                                : "Confirmar nova fila"
                )

                .setMessage(
                        texto.toString()
                )

                .setPositiveButton(
                        "INICIAR",
                        (dialog, which) ->
                                prepararFila(
                                        selecionados,
                                        distribuicao
                                )
                )

                .setNegativeButton(
                        "CANCELAR",
                        null
                )

                .show();
    }

    private void prepararFila(
            ArrayList<Contato> selecionados,
            Map<String, Versiculo> distribuicao
    ) {

        pendenteNomes =
                new ArrayList<>();

        pendenteNumeros =
                new ArrayList<>();

        pendenteSalmos =
                new ArrayList<>();

        pendenteVersiculos =
                new ArrayList<>();

        pendenteTextos =
                new ArrayList<>();


        for (
                Contato contato :
                selecionados
        ) {

            Versiculo v =
                    distribuicao.get(
                            contato.numero
                    );


            if (
                    v == null
            ) {

                continue;
            }


            pendenteNomes.add(
                    contato.nome
            );


            pendenteNumeros.add(
                    contato.numero
            );


            pendenteSalmos.add(
                    v.salmo
            );


            pendenteVersiculos.add(
                    v.versiculo
            );


            pendenteTextos.add(
                    v.texto
            );
        }


        if (
                pendenteNomes.isEmpty()
        ) {

            return;
        }


        pedirOverlay(
                ACAO_NOVA_FILA
        );
    }


    private void continuarFila() {

        if (
                !MessageService
                        .existeFilaPendente(
                                this
                        )
        ) {

            Toast.makeText(
                    this,
                    "Não existe fila pendente.",
                    Toast.LENGTH_LONG
            ).show();

            botaoContinuar.setEnabled(
                    false
            );

            return;
        }


        pedirOverlay(
                ACAO_CONTINUAR
        );
    }


    private void pedirOverlay(
            int acao
    ) {

        if (
                Build.VERSION.SDK_INT < 23
                || Settings.canDrawOverlays(
                this
        )
        ) {

            executarAcaoOverlay(
                    acao
            );

            return;
        }


        acaoDepoisOverlay =
                acao;


        new AlertDialog.Builder(this)

                .setTitle(
                        "Permissão necessária"
                )

                .setMessage(
                        "Para mostrar os botões PRÓXIMO "
                                + "e ENCERRAR sobre o WhatsApp, "
                                + "permita \"Exibir sobre outros apps\" "
                                + "para Koinonia Salmos.\n\n"
                                + "Essa autorização é necessária "
                                + "somente uma vez."
                )

                .setPositiveButton(
                        "ABRIR CONFIGURAÇÃO",
                        (dialog, which) -> {

                            Intent intent =
                                    new Intent(
                                            Settings
                                                    .ACTION_MANAGE_OVERLAY_PERMISSION,

                                            Uri.parse(
                                                    "package:"
                                                            + getPackageName()
                                            )
                                    );


                            startActivity(
                                    intent
                            );
                        }
                )

                .setNegativeButton(
                        "CANCELAR",
                        (dialog, which) ->
                                acaoDepoisOverlay =
                                        ACAO_NENHUMA
                )

                .show();
    }


    private void executarAcaoOverlay(
            int acao
    ) {

        if (
                acao
                        == ACAO_NOVA_FILA
        ) {

            iniciarNovaFila();

        } else if (
                acao
                        == ACAO_CONTINUAR
        ) {

            continuarServico();
        }
    }


    private void iniciarNovaFila() {

        Intent intent =
                new Intent(
                        this,
                        MessageService.class
                );


        intent.setAction(
                MessageService.ACTION_START
        );


        intent.putStringArrayListExtra(
                "nomes",
                pendenteNomes
        );


        intent.putStringArrayListExtra(
                "numeros",
                pendenteNumeros
        );


        intent.putIntegerArrayListExtra(
                "salmos",
                pendenteSalmos
        );


        intent.putIntegerArrayListExtra(
                "versiculos",
                pendenteVersiculos
        );


        intent.putStringArrayListExtra(
                "textos",
                pendenteTextos
        );


        if (
                Build.VERSION.SDK_INT
                        >= 26
        ) {

            startForegroundService(
                    intent
            );

        } else {

            startService(
                    intent
            );
        }


        botaoContinuar.setEnabled(
                true
        );
    }


    private void continuarServico() {

        Intent intent =
                new Intent(
                        this,
                        MessageService.class
                );


        intent.setAction(
                MessageService.ACTION_RESUME
        );


        if (
                Build.VERSION.SDK_INT
                        >= 26
        ) {

            startForegroundService(
                    intent
            );

        } else {

            startService(
                    intent
            );
        }
    }


    @Override
    protected void onResume() {

        super.onResume();


        if (
                acaoDepoisOverlay
                        != ACAO_NENHUMA
                && (
                Build.VERSION.SDK_INT < 23
                        || Settings.canDrawOverlays(
                        this
                )
        )
        ) {

            int acao =
                    acaoDepoisOverlay;


            acaoDepoisOverlay =
                    ACAO_NENHUMA;


            executarAcaoOverlay(
                    acao
            );
        }


        if (
                botaoContinuar
                        != null
        ) {

            botaoContinuar.setEnabled(
                    MessageService
                            .existeFilaPendente(
                                    this
                            )
            );
        }
    }


    private void salvarUltimaDistribuicao(
            Map<String, Versiculo> mapa
    ) {

        try {

            JSONObject raiz =
                    new JSONObject();


            for (
                    Map.Entry<String, Versiculo> item :
                    mapa.entrySet()
            ) {

                Versiculo v =
                        item.getValue();


                JSONObject obj =
                        new JSONObject();


                obj.put(
                        "salmo",
                        v.salmo
                );


                obj.put(
                        "versiculo",
                        v.versiculo
                );


                obj.put(
                        "texto",
                        v.texto
                );


                raiz.put(
                        item.getKey(),
                        obj
                );
            }


            getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE
            )
                    .edit()

                    .putString(
                            ULTIMA_DISTRIBUICAO,
                            raiz.toString()
                    )

                    .apply();


            botaoRepetir.setEnabled(
                    true
            );

        } catch (Exception ignored) {
        }
    }


    private Map<String, Versiculo>
    carregarUltimaDistribuicao() {

        LinkedHashMap<String, Versiculo> resultado =
                new LinkedHashMap<>();


        String salvo =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                )
                        .getString(
                                ULTIMA_DISTRIBUICAO,
                                null
                        );


        if (
                salvo == null
        ) {

            return resultado;
        }


        try {

            JSONObject raiz =
                    new JSONObject(
                            salvo
                    );


            JSONArray nomes =
                    raiz.names();


            if (
                    nomes == null
            ) {

                return resultado;
            }


            for (
                    int i = 0;
                    i < nomes.length();
                    i++
            ) {

                String numero =
                        nomes.getString(i);


                JSONObject obj =
                        raiz.getJSONObject(
                                numero
                        );


                resultado.put(
                        numero,

                        new Versiculo(
                                obj.getInt(
                                        "salmo"
                                ),

                                obj.getInt(
                                        "versiculo"
                                ),

                                obj.getString(
                                        "texto"
                                )
                        )
                );
            }

        } catch (Exception ignored) {
        }


        return resultado;
    }


    private boolean existeUltimaDistribuicao() {

        return getSharedPreferences(
                PREFS,
                MODE_PRIVATE
        )
                .contains(
                        ULTIMA_DISTRIBUICAO
                );
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
}
