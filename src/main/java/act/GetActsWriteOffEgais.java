package act;

import java.io.File;
import java.io.PrintStream;
import java.time.LocalDate;

import core.ActHeader;
import core.GenerationException;
import core.GenerationResult;
import core.WriteOffService;
import helpers.Constants;
import helpers.XmlSupport;

/**
 * Консольный режим формирования актов списания — удобен для запуска по расписанию
 * и для пакетной обработки.
 * <p>
 * Пример:
 * <pre>
 * java -jar egais-writeoff.jar --in ReplyRests_v2.xml --out C:\Acts ^
 *      --number 10 --date 24-09-2024 --strong --beer
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
        String fsrarId = null;
        String actNumber = null;
        String beerActNumber = null;
        String actDateText = null;
        String typeWriteOff = null;
        String note = null;
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
                    case "--fsrar-id":
                        fsrarId = nextValue(args, ++i, arg);
                        break;
                    case "--number":
                        actNumber = nextValue(args, ++i, arg);
                        break;
                    case "--number-beer":
                        beerActNumber = nextValue(args, ++i, arg);
                        break;
                    case "--date":
                        actDateText = nextValue(args, ++i, arg);
                        break;
                    case "--type":
                        typeWriteOff = nextValue(args, ++i, arg);
                        break;
                    case "--note":
                        note = nextValue(args, ++i, arg);
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

        if (isBlank(fsrarId)) {
            // Код организации есть в самой выгрузке остатков, поэтому отдельный
            // аргумент нужен только если его требуется переопределить.
            try {
                fsrarId = XmlSupport.ownerFsrarId(inputFile);
            } catch (GenerationException e) {
                System.err.println("Ошибка: " + e.getMessage());
                return 1;
            }
            if (!fsrarId.isEmpty()) {
                System.out.println("FSRAR_ID взят из файла остатков: " + fsrarId);
            }
        }

        ActHeader strongHeader = null;
        ActHeader beerHeader = null;
        try {
            LocalDate actDate = ActHeader.parseDate(isBlank(actDateText)
                    ? LocalDate.now().format(ActHeader.USER_DATE)
                    : actDateText);
            String type = typeWriteOff != null ? typeWriteOff : Constants.DEFAULT_TYPE_WRITE_OFF;
            String text = note != null ? note : ActHeader.defaultNote(actDate);
            // Номера актов задаются отдельно; если номер для пива не указан,
            // используется тот же номер, что и для крепкого алкоголя.
            String beerNumber = isBlank(beerActNumber) ? actNumber : beerActNumber;
            if (strong) {
                strongHeader = new ActHeader(fsrarId, actNumber, actDate, type, text);
                strongHeader.validate();
            }
            if (beer) {
                beerHeader = new ActHeader(fsrarId, beerNumber, actDate, type, text);
                beerHeader.validate();
            }
        } catch (GenerationException e) {
            System.err.println("Ошибка: " + e.getMessage());
            printUsage(System.err);
            return 2;
        }

        System.out.println(Constants.APP_NAME + " " + Constants.APP_VERSION);
        try {
            WriteOffService service = new WriteOffService().withLogger(System.out::println);
            GenerationResult result = service.generate(inputFile, outputDir, strongHeader, beerHeader);
            System.out.println("Готово. Обработано позиций: " + result.getTotalPositions()
                    + " (крепкий алкоголь — " + result.getStrongPositions()
                    + ", пиво — " + result.getBeerPositions() + ").");
            return 0;
        } catch (GenerationException e) {
            System.err.println("Ошибка: " + e.getMessage());
            return 1;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
        out.println("  java -jar egais-writeoff.jar --in <файл остатков> --out <папка> --number <номер>");
        out.println("      [--number-beer <номер>] [--fsrar-id <код>] [--date <ДД-ММ-ГГГГ>]");
        out.println("      [--type <причина>] [--note <текст>] [--strong] [--beer]");
        out.println("      — сформировать акты без интерфейса.");
        out.println();
        out.println("  --in       <файл>          выгрузка остатков ЕГАИС (ReplyRests_v2.xml)");
        out.println("  --out      <папка>         папка для сохранения актов списания");
        out.println("  --number   <номер>         номер акта по крепкому алкоголю (он же awr:Identity)");
        out.println("  --number-beer <номер>      номер акта по пиву; по умолчанию — как --number");
        out.println("  --fsrar-id <код>           FSRAR_ID организации; если не указан, берётся из файла остатков");
        out.println("  --date     <ДД-ММ-ГГГГ>    дата списания, по умолчанию — сегодняшняя");
        out.println("  --type     <причина>       причина списания, по умолчанию — «"
                + Constants.DEFAULT_TYPE_WRITE_OFF + "»");
        out.println("  --note     <текст>         примечание, по умолчанию — «"
                + ActHeader.defaultNote(LocalDate.now()) + "»");
        out.println("  --strong                   только акт по крепкому алкоголю");
        out.println("  --beer                     только акт по пиву");
        out.println("  --help                     эта подсказка");
        out.println();
        out.println("Если не указан ни --strong, ни --beer, формируются оба акта.");
    }
}
