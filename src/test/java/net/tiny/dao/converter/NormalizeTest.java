package net.tiny.dao.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class NormalizeTest {

    static class NFKCArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                Arguments.of("・Ｎａｍｅ１　！\"＃＄％＆（）＊＋，－．／：；＜＝＞？＠［］＾＿｀｛｜｝￥", "・Name1 !\"#$%&()*+,-./:;<=>?@[]^_`{|}¥"),
                Arguments.of("ｱｶﾞｨ", "アガィ")
            );
        }
    }

    @DisplayName("Normalize NKFC")
    @ParameterizedTest(name = "{index} => in={0}, out={1}")
    @ArgumentsSource(NFKCArgumentProvider.class)
    public void testNFKCNormalizedString(String input, String expected) {
        assertEquals(expected, Normalize.normalize(input));
    }

    static class TelArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                Arguments.of("０３-６４４０-６０００" , "0364406000"),
                Arguments.of("03　６ ４４０-６0０0", "0364406000")
            );
        }
    }

    @DisplayName("Normalize Tel number")
    @ParameterizedTest(name = "{index} => in={0}, out={1}")
    @ArgumentsSource(TelArgumentProvider.class)
    public void testTelNormalizedString(String input, String expected) {
        Normalize normalize = new Normalize();
        assertEquals(expected, normalize.tel(input));
    }

    static class AddressArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                Arguments.of("1丁目12番3" , "1-12-3"),
                Arguments.of("1丁目12番地3号", "1-12-3"),
                Arguments.of("1丁目12番地3", "1-12-3"),
                Arguments.of("1丁目12番3号", "1-12-3"),
                Arguments.of("1丁目12番地", "1-12"),
                Arguments.of("1丁目12番", "1-12"),
                Arguments.of("1丁目12", "1-12"),
                Arguments.of("1丁目左3号", "1-3"),
                Arguments.of("1丁目右3号", "1-3"),
                Arguments.of("1丁目左3", "1-3"),
                Arguments.of("1丁目右3", "1-3"),
                Arguments.of("12番の3号", "12-3"),
                Arguments.of("12番地3", "12-3"),
                Arguments.of("12番地3号", "12-3"),
                Arguments.of("12番3号", "12-3"),
                Arguments.of("12番3", "12-3"),
                Arguments.of("12の3", "12-3"),
                Arguments.of("1丁目", "1"),
                Arguments.of("1番地", "1"),
                Arguments.of("1番", "1"),
                Arguments.of("1号", "1"),
                Arguments.of("福岡県福岡市中央区天神4丁目3番地30号", "福岡県福岡市中央区天神4-3-30"),
                Arguments.of("東京都千代田区紀尾井町１−３ 紀尾井タワー", "東京都千代田区紀尾井町1-3紀尾井タワー")
            );
        }
    }

    @DisplayName("Normalize Address")
    @ParameterizedTest(name = "{index} => in={0}, out={1}")
    @ArgumentsSource(AddressArgumentProvider.class)
    public void testAddressNormalizedString(String input, String expected) {
        Normalize normalize = new Normalize();
        assertEquals(expected, normalize.address(input));
    }
}
