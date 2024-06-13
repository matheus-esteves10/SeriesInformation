package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.DadosEpisodio;
import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner scanner = new Scanner(System.in);
    private  ConsumoApi consumoApi = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "PUT_YOUR_API_KEY_HERE";
    private final DecimalFormat df = new DecimalFormat("#.00");

    public void exibeMenu() {
        System.out.println("Digite o nome da série para busca: ");
        var nomeSerie = scanner.nextLine();
        var json = consumoApi.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dadosSerie = conversor.obterDados(json, DadosSerie.class);
        System.out.println(dadosSerie);

        List<DadosTemporada> temporadas = new ArrayList<>();

        //separa os dados por temporada
        for(int i = 1; i<=dadosSerie.totalTemporadas(); i++) {
			json = consumoApi.obterDados (ENDERECO + nomeSerie.replace(" ", "+") +"&season=" + i + API_KEY);
			DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
			temporadas.add(dadosTemporada);

		}
//		temporadas.forEach(System.out::println); //escreve todos os dados separados acima

        //imprime todos os episódios
        //        for (int i = 0; i < dadosSerie.totalTemporadas(); i++) {
//            List <DadosEpisodio> episodiosTemporada = temporadas.get(i).episodio();
//            for (int j = 0; j < episodiosTemporada.size(); j++) {
//                System.out.println(episodiosTemporada.get(j).titulo());
//            }
//        }

        //temporadas.forEach(t -> t.episodio().forEach(e -> System.out.println(e.titulo()))); //mesma coisa do for acima mas com stream

        List <DadosEpisodio> dadosEpisodios = temporadas.stream()
                .flatMap(t -> t.episodio().stream())
                //.toList() imutável
                .collect(Collectors.toList()); //mutável

        //UTILIZAÇÃO DO .PEEK CASO QUEIRA DEBUGAR UM STREAM


        System.out.println("\nTodos os episódios da série " + dadosSerie.titulo() + ":"); //Mostra como se fosse um catálogo
        List<Episodio> episodios = temporadas.stream()
                .flatMap(t -> t.episodio().stream()
                        .map(d -> new Episodio(t.numeroTemporada(), d))
                ).collect(Collectors.toList());

        episodios.forEach(System.out::println);

        System.out.println("\nTop 5 episódios da série " + dadosSerie.titulo() + ":");
        dadosEpisodios.stream()
                .filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
                .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
                .limit(5)
                .forEach(System.out::println);

        System.out.println("\nQual episódio você deseja filtrar?: ");
        var trechoTitulo = scanner.nextLine();
        Optional<Episodio> episodioBuscado = episodios.stream() //optional -> pode ser que encontre, pode ser que não encontre
                .filter(e -> e.getTitulo().toUpperCase().contains(trechoTitulo.toUpperCase()))
                .findFirst();
        if(episodioBuscado.isPresent()){
            System.out.println("\nEpisódio encontrado!");
            System.out.println("Temporada: " + episodioBuscado.get().getNumeroTemporada());
            System.out.println("Episódio: " + episodioBuscado.get().getNumeroEpisodio());
        } else {
            System.out.println("Episódio não encontrado!");
        }

        System.out.println("\nA partir de que ano você deseja ver os episódios? ");
        var ano = scanner.nextInt();
        scanner.nextLine();

        LocalDate dataBusca = LocalDate.of(ano, 1, 1);
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        System.out.println("\nEpisódios a partir de " + dataBusca);
        episodios.stream()
                .filter(e -> e.getDataLancamento() != null && e.getDataLancamento().isAfter(dataBusca))
                .forEach(e -> System.out.println(
                        "Temporada: " + e.getNumeroTemporada() +
                                " Episódio: " + e.getTitulo() +
                                " Data de Lançamento: " + e.getDataLancamento().format(formatador)

                ));

        System.out.println("\nMédia das temporadas de " + dadosSerie.titulo());
        Map<Integer, Double> avaliacoesPorTemporada = episodios.stream()
                .filter(e -> e.getAvaliacao() > 0.0)
                .collect(Collectors.groupingBy(Episodio::getNumeroTemporada,
                        Collectors.averagingDouble(Episodio::getAvaliacao)));//ideia chave e valor

        avaliacoesPorTemporada.forEach((temporada, media) -> {
            System.out.println("Temporada " + temporada + ": " + df.format(media));
        });

        System.out.println("\nEstátisticas gerais da série " + dadosSerie.titulo());
        DoubleSummaryStatistics est = episodios.stream()
                .filter(e -> e.getAvaliacao() > 0.0)
                .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));
        System.out.println("Média: " + df.format(est.getAverage()));
        System.out.println("Melhor episódio: " + est.getMax());
        System.out.println("Pior episódio: " + est.getMin());
        System.out.println("Quaintidade de episódios avaliados: " + est.getCount());
    }
}
