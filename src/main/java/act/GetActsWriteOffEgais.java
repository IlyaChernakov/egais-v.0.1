package act;

import java.io.File;
import java.io.PrintStream;

import core.GenerationException;
import core.GenerationResult;
import core.WriteOffService;
import helpers.Constants;

/**
 * Консольный режим формирования актов списания — удобен для запуска по расписанию
 * и для пакетной обработки.
 * <p>
 * Пример:
 * <pre>
 * java -jar egais-writeoff.jar --in ReplyRests_v2.xml --out C:\Acts --strong --beer
 * </pre>
 */
public class GetActsWriteOffEgais {

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /**
     * Выполняет разбор аргументов и формирование актов.
     *
     * @param args аргументы командной строки
     * @return код возврата: 0 — успех, 1 — ошибка данных, 2 — неверные аргументы
     */
    public static int run(String[] args) {
        File inputFile = null;
        File outputDir = null;
        boolean strong = false;
        boolean beer = false;
        boolean anyKindSelected = false;

        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--in":
                    case "-i":
                        inputFile = new File(nextValue(args, ++i, arg));
                        break;
                    case "--out":
                    case "-o":
                        outputDir = new File(nextValue(args, ++i, arg));
                        break;
                    case "--strong":
                        strong = true;
                        anyKindSelected = true;
                        break;
                    case "--beer":
                        beer = true;
                        anyKindSelected = true;
                        break;
                    case "--help":
                    case "-h":
                    case "/?":
                        printUsage(System.out);
                        return 0;
                    default:
                        System.err.println("Неизвестный аргумент: " + arg);
                        printUsage(System.err);
                        return 2;
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            printUsage(System.err);
            return 2;
        }

        if (inputFile == null || outputDir == null) {
            printUsage(System.err);
            return 2;
        }
        if (!anyKindSelected) {
            // По умолчанию формируются оба акта — как в исходной версии программы.
            strong = true;
            beer = true;
        }

        System.out.println(Constants.APP_NAME + " " + Constants.APP_VERSION);
        try {
            WriteOffService service = new WriteOffService().withLogger(System.out::println);
            GenerationResult result = service.generate(inputFile, outputDir, strong, beer);
            System.out.println("Готово. Обработано позиций: " + result.getTotalPositions()
                    + " (крепкий алкоголь — " + result.getStrongPositions()
                    + ", пиво — " + result.getBeerPositions() + ").");
            return 0;
        } catch (GenerationException e) {
            System.err.println("Ошибка: " + e.getMessage());
            return 1;
        }
    }

    private static String nextValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Не указано значение для " + option);
        }
        return args[index];
    }

    /**
     * Печатает подсказку по аргументам командной строки.
     *
     * @param out поток вывода
     */
    public static void printUsage(PrintStream out) {
        out.println("Использование:");
        out.println("  java -jar egais-writeoff.jar");
        out.println("      — открыть графический интерфейс;");
        out.println("  java -jar egais-writeoff.jar --in <файл остатков> --out <папка> [--strong] [--beer]");
        out.println("      — сформировать акты без интерфейса.");
        out.println();
        out.println("  --in  <файл>   выгрузка остатков ЕГАИС (ReplyRests_v2.xml)");
        out.println("  --out <папка>  папка для сохранения актов списания");
        out.println("  --strong       только акт по крепкому алкоголю");
        out.println("  --beer         только акт по пиву");
        out.println("  --help         эта подсказка");
        out.println();
        out.println("Если не указан ни --strong, ни --beer, формируются оба акта.");
    }
}
