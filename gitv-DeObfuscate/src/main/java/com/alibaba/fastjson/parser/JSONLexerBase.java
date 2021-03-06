package com.alibaba.fastjson.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.asm.Opcodes;
import com.alibaba.fastjson.util.IOUtils;
import com.gala.pingback.IPingbackFactory;
import com.gala.video.app.epg.ui.albumlist.constant.IAlbumConfig;
import com.gala.video.lib.share.common.configs.HomeDataConfig.ItemSize;
import com.gala.video.lib.share.ifimpl.ucenter.history.impl.HistoryInfoHelper;
import com.gala.video.lib.share.uikit.loader.UikitEventType;
import java.io.Closeable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import org.xbill.DNS.TTL;
import org.xbill.DNS.WKSRecord.Protocol;
import org.xbill.DNS.WKSRecord.Service;

public abstract class JSONLexerBase implements JSONLexer, Closeable {
    protected static final int INT_MULTMIN_RADIX_TEN = -214748364;
    protected static final long MULTMIN_RADIX_TEN = -922337203685477580L;
    private static final ThreadLocal<char[]> SBUF_LOCAL = new ThreadLocal();
    protected static final int[] digits = new int[103];
    protected static final char[] typeFieldName = ("\"" + JSON.DEFAULT_TYPE_KEY + "\":\"").toCharArray();
    protected int bp;
    protected Calendar calendar = null;
    protected char ch;
    protected int eofPos;
    protected int features;
    protected boolean hasSpecial;
    protected Locale locale = JSON.defaultLocale;
    public int matchStat = 0;
    protected int np;
    protected int pos;
    protected char[] sbuf;
    protected int sp;
    protected String stringDefaultValue = null;
    protected TimeZone timeZone = JSON.defaultTimeZone;
    protected int token;

    public abstract String addSymbol(int i, int i2, int i3, SymbolTable symbolTable);

    protected abstract void arrayCopy(int i, char[] cArr, int i2, int i3);

    public abstract byte[] bytesValue();

    protected abstract boolean charArrayCompare(char[] cArr);

    public abstract char charAt(int i);

    protected abstract void copyTo(int i, int i2, char[] cArr);

    public abstract int indexOf(char c, int i);

    public abstract boolean isEOF();

    public abstract char next();

    public abstract String numberString();

    public abstract String stringVal();

    public abstract String subString(int i, int i2);

    protected abstract char[] sub_chars(int i, int i2);

    protected void lexError(String key, Object... args) {
        this.token = 1;
    }

    static {
        int i;
        for (i = 48; i <= 57; i++) {
            digits[i] = i - 48;
        }
        for (i = 97; i <= 102; i++) {
            digits[i] = (i - 97) + 10;
        }
        for (i = 65; i <= 70; i++) {
            digits[i] = (i - 65) + 10;
        }
    }

    public JSONLexerBase(int features) {
        this.features = features;
        if ((Feature.InitStringFieldAsEmpty.mask & features) != 0) {
            this.stringDefaultValue = "";
        }
        this.sbuf = (char[]) SBUF_LOCAL.get();
        if (this.sbuf == null) {
            this.sbuf = new char[512];
        }
    }

    public final int matchStat() {
        return this.matchStat;
    }

    public void setToken(int token) {
        this.token = token;
    }

    public final void nextToken() {
        this.sp = 0;
        while (true) {
            this.pos = this.bp;
            if (this.ch == '/') {
                skipComment();
            } else if (this.ch == '\"') {
                scanString();
                return;
            } else if (this.ch == ',') {
                next();
                this.token = 16;
                return;
            } else if (this.ch >= '0' && this.ch <= '9') {
                scanNumber();
                return;
            } else if (this.ch == '-') {
                scanNumber();
                return;
            } else {
                switch (this.ch) {
                    case '\b':
                    case '\t':
                    case '\n':
                    case '\f':
                    case '\r':
                    case ' ':
                        next();
                        break;
                    case '\'':
                        if (isEnabled(Feature.AllowSingleQuotes)) {
                            scanStringSingleQuote();
                            return;
                        }
                        throw new JSONException("Feature.AllowSingleQuotes is false");
                    case IPingbackFactory.BOTTOM_EPISODE_CLICK /*40*/:
                        next();
                        this.token = 10;
                        return;
                    case ')':
                        next();
                        this.token = 11;
                        return;
                    case Opcodes.ASTORE /*58*/:
                        next();
                        this.token = 17;
                        return;
                    case Protocol.WB_MON /*78*/:
                    case 'S':
                    case 'T':
                    case Service.UUCP_PATH /*117*/:
                        scanIdent();
                        return;
                    case Service.MIT_DOV /*91*/:
                        next();
                        this.token = 14;
                        return;
                    case Service.DCP /*93*/:
                        next();
                        this.token = 15;
                        return;
                    case 'f':
                        scanFalse();
                        return;
                    case 'n':
                        scanNullOrNew();
                        return;
                    case 't':
                        scanTrue();
                        return;
                    case Service.NTP /*123*/:
                        next();
                        this.token = 12;
                        return;
                    case Service.LOCUS_MAP /*125*/:
                        next();
                        this.token = 13;
                        return;
                    default:
                        if (isEOF()) {
                            if (this.token == 20) {
                                throw new JSONException("EOF error");
                            }
                            this.token = 20;
                            int i = this.eofPos;
                            this.bp = i;
                            this.pos = i;
                            return;
                        } else if (this.ch <= '\u001f' || this.ch == '') {
                            next();
                            break;
                        } else {
                            lexError("illegal.char", String.valueOf(this.ch));
                            next();
                            return;
                        }
                }
            }
        }
    }

    public final void nextToken(int expect) {
        this.sp = 0;
        while (true) {
            switch (expect) {
                case 2:
                    if (this.ch >= '0' && this.ch <= '9') {
                        this.pos = this.bp;
                        scanNumber();
                        return;
                    } else if (this.ch == '\"') {
                        this.pos = this.bp;
                        scanString();
                        return;
                    } else if (this.ch == '[') {
                        this.token = 14;
                        next();
                        return;
                    } else if (this.ch == '{') {
                        this.token = 12;
                        next();
                        return;
                    }
                    break;
                case 4:
                    if (this.ch == '\"') {
                        this.pos = this.bp;
                        scanString();
                        return;
                    } else if (this.ch >= '0' && this.ch <= '9') {
                        this.pos = this.bp;
                        scanNumber();
                        return;
                    } else if (this.ch == '[') {
                        this.token = 14;
                        next();
                        return;
                    } else if (this.ch == '{') {
                        this.token = 12;
                        next();
                        return;
                    }
                    break;
                case 12:
                    if (this.ch == '{') {
                        this.token = 12;
                        next();
                        return;
                    } else if (this.ch == '[') {
                        this.token = 14;
                        next();
                        return;
                    }
                    break;
                case 14:
                    if (this.ch == '[') {
                        this.token = 14;
                        next();
                        return;
                    } else if (this.ch == '{') {
                        this.token = 12;
                        next();
                        return;
                    }
                    break;
                case 15:
                    if (this.ch == ']') {
                        this.token = 15;
                        next();
                        return;
                    }
                    break;
                case 16:
                    if (this.ch == ',') {
                        this.token = 16;
                        next();
                        return;
                    } else if (this.ch == '}') {
                        this.token = 13;
                        next();
                        return;
                    } else if (this.ch == ']') {
                        this.token = 15;
                        next();
                        return;
                    } else if (this.ch == JSONLexer.EOI) {
                        this.token = 20;
                        return;
                    }
                    break;
                case 18:
                    nextIdent();
                    return;
                case 20:
                    break;
            }
            if (this.ch == JSONLexer.EOI) {
                this.token = 20;
                return;
            }
            if (this.ch == ' ' || this.ch == '\n' || this.ch == '\r' || this.ch == '\t' || this.ch == '\f' || this.ch == '\b') {
                next();
            } else {
                nextToken();
                return;
            }
        }
    }

    public final void nextIdent() {
        while (isWhitespace(this.ch)) {
            next();
        }
        if (this.ch == '_' || Character.isLetter(this.ch)) {
            scanIdent();
        } else {
            nextToken();
        }
    }

    public final void nextTokenWithColon() {
        nextTokenWithChar(':');
    }

    public final void nextTokenWithChar(char expect) {
        this.sp = 0;
        while (this.ch != expect) {
            if (this.ch == ' ' || this.ch == '\n' || this.ch == '\r' || this.ch == '\t' || this.ch == '\f' || this.ch == '\b') {
                next();
            } else {
                throw new JSONException("not match " + expect + " - " + this.ch);
            }
        }
        next();
        nextToken();
    }

    public final int token() {
        return this.token;
    }

    public final String tokenName() {
        return JSONToken.name(this.token);
    }

    public final int pos() {
        return this.pos;
    }

    public final String stringDefaultValue() {
        return this.stringDefaultValue;
    }

    public final Number integerValue() throws NumberFormatException {
        long limit;
        int i;
        long result = 0;
        boolean negative = false;
        if (this.np == -1) {
            this.np = 0;
        }
        int i2 = this.np;
        int max = this.np + this.sp;
        char type = ' ';
        switch (charAt(max - 1)) {
            case 'B':
                max--;
                type = 'B';
                break;
            case Protocol.BR_SAT_MON /*76*/:
                max--;
                type = 'L';
                break;
            case 'S':
                max--;
                type = 'S';
                break;
        }
        if (charAt(this.np) == '-') {
            negative = true;
            limit = Long.MIN_VALUE;
            i = i2 + 1;
        } else {
            limit = -9223372036854775807L;
            i = i2;
        }
        if (i < max) {
            result = (long) (-(charAt(i) - 48));
            i++;
        }
        while (i < max) {
            i2 = i + 1;
            int digit = charAt(i) - 48;
            if (result < MULTMIN_RADIX_TEN) {
                return new BigInteger(numberString());
            }
            result *= 10;
            if (result < ((long) digit) + limit) {
                return new BigInteger(numberString());
            }
            result -= (long) digit;
            i = i2;
        }
        if (!negative) {
            result = -result;
            if (result > TTL.MAX_VALUE || type == 'L') {
                i2 = i;
                return Long.valueOf(result);
            } else if (type == 'S') {
                i2 = i;
                return Short.valueOf((short) ((int) result));
            } else if (type == 'B') {
                i2 = i;
                return Byte.valueOf((byte) ((int) result));
            } else {
                i2 = i;
                return Integer.valueOf((int) result);
            }
        } else if (i <= this.np + 1) {
            throw new NumberFormatException(numberString());
        } else if (result < -2147483648L || type == 'L') {
            i2 = i;
            return Long.valueOf(result);
        } else if (type == 'S') {
            i2 = i;
            return Short.valueOf((short) ((int) result));
        } else if (type == 'B') {
            i2 = i;
            return Byte.valueOf((byte) ((int) result));
        } else {
            i2 = i;
            return Integer.valueOf((int) result);
        }
    }

    public final void nextTokenWithColon(int expect) {
        nextTokenWithChar(':');
    }

    public float floatValue() {
        return Float.parseFloat(numberString());
    }

    public double doubleValue() {
        return Double.parseDouble(numberString());
    }

    public void config(Feature feature, boolean state) {
        this.features = Feature.config(this.features, feature, state);
        if ((this.features & Feature.InitStringFieldAsEmpty.mask) != 0) {
            this.stringDefaultValue = "";
        }
    }

    public final boolean isEnabled(Feature feature) {
        return isEnabled(feature.mask);
    }

    public final boolean isEnabled(int feature) {
        return (this.features & feature) != 0;
    }

    public final char getCurrent() {
        return this.ch;
    }

    protected void skipComment() {
        next();
        if (this.ch == '/') {
            do {
                next();
            } while (this.ch != '\n');
            next();
        } else if (this.ch == '*') {
            next();
            while (this.ch != JSONLexer.EOI) {
                if (this.ch == '*') {
                    next();
                    if (this.ch == '/') {
                        next();
                        return;
                    }
                } else {
                    next();
                }
            }
        } else {
            throw new JSONException("invalid comment");
        }
    }

    public final String scanSymbol(SymbolTable symbolTable) {
        skipWhitespace();
        if (this.ch == '\"') {
            return scanSymbol(symbolTable, '\"');
        }
        if (this.ch == '\'') {
            if (isEnabled(Feature.AllowSingleQuotes)) {
                return scanSymbol(symbolTable, '\'');
            }
            throw new JSONException("syntax error");
        } else if (this.ch == '}') {
            next();
            this.token = 13;
            return null;
        } else if (this.ch == ',') {
            next();
            this.token = 16;
            return null;
        } else if (this.ch == JSONLexer.EOI) {
            this.token = 20;
            return null;
        } else if (isEnabled(Feature.AllowUnQuotedFieldNames)) {
            return scanSymbolUnQuoted(symbolTable);
        } else {
            throw new JSONException("syntax error");
        }
    }

    public final String scanSymbol(SymbolTable symbolTable, char quote) {
        int hash = 0;
        this.np = this.bp;
        this.sp = 0;
        boolean hasSpecial = false;
        while (true) {
            char chLocal = next();
            if (chLocal == quote) {
                String value;
                this.token = 4;
                if (hasSpecial) {
                    value = symbolTable.addSymbol(this.sbuf, 0, this.sp, hash);
                } else {
                    int offset;
                    if (this.np == -1) {
                        offset = 0;
                    } else {
                        offset = this.np + 1;
                    }
                    value = addSymbol(offset, this.sp, hash, symbolTable);
                }
                this.sp = 0;
                next();
                return value;
            } else if (chLocal == '\u001a') {
                throw new JSONException("unclosed.str");
            } else if (chLocal == '\\') {
                if (!hasSpecial) {
                    hasSpecial = true;
                    if (this.sp >= this.sbuf.length) {
                        int newCapcity = this.sbuf.length * 2;
                        if (this.sp > newCapcity) {
                            newCapcity = this.sp;
                        }
                        char[] newsbuf = new char[newCapcity];
                        System.arraycopy(this.sbuf, 0, newsbuf, 0, this.sbuf.length);
                        this.sbuf = newsbuf;
                    }
                    arrayCopy(this.np + 1, this.sbuf, 0, this.sp);
                }
                chLocal = next();
                switch (chLocal) {
                    case '\"':
                        hash = (hash * 31) + 34;
                        putChar('\"');
                        break;
                    case '\'':
                        hash = (hash * 31) + 39;
                        putChar('\'');
                        break;
                    case '/':
                        hash = (hash * 31) + 47;
                        putChar('/');
                        break;
                    case '0':
                        hash = (hash * 31) + chLocal;
                        putChar('\u0000');
                        break;
                    case '1':
                        hash = (hash * 31) + chLocal;
                        putChar('\u0001');
                        break;
                    case '2':
                        hash = (hash * 31) + chLocal;
                        putChar('\u0002');
                        break;
                    case '3':
                        hash = (hash * 31) + chLocal;
                        putChar('\u0003');
                        break;
                    case '4':
                        hash = (hash * 31) + chLocal;
                        putChar('\u0004');
                        break;
                    case '5':
                        hash = (hash * 31) + chLocal;
                        putChar('\u0005');
                        break;
                    case '6':
                        hash = (hash * 31) + chLocal;
                        putChar('\u0006');
                        break;
                    case '7':
                        hash = (hash * 31) + chLocal;
                        putChar('\u0007');
                        break;
                    case UikitEventType.UIKIT_ADD_DETAIL_CARDS /*70*/:
                    case 'f':
                        hash = (hash * 31) + 12;
                        putChar('\f');
                        break;
                    case '\\':
                        hash = (hash * 31) + 92;
                        putChar('\\');
                        break;
                    case Service.TACNEWS /*98*/:
                        hash = (hash * 31) + 8;
                        putChar('\b');
                        break;
                    case 'n':
                        hash = (hash * 31) + 10;
                        putChar('\n');
                        break;
                    case HistoryInfoHelper.MSG_CACHE_CLEAR_ALL /*114*/:
                        hash = (hash * 31) + 13;
                        putChar('\r');
                        break;
                    case 't':
                        hash = (hash * 31) + 9;
                        putChar('\t');
                        break;
                    case Service.UUCP_PATH /*117*/:
                        char c1 = next();
                        char c2 = next();
                        char c3 = next();
                        char c4 = next();
                        int val = Integer.parseInt(new String(new char[]{c1, c2, c3, c4}), 16);
                        hash = (hash * 31) + val;
                        putChar((char) val);
                        break;
                    case ItemSize.ITEM_118 /*118*/:
                        hash = (hash * 31) + 11;
                        putChar('\u000b');
                        break;
                    case 'x':
                        char x1 = next();
                        this.ch = x1;
                        char x2 = next();
                        this.ch = x2;
                        char x_char = (char) ((digits[x1] * 16) + digits[x2]);
                        hash = (hash * 31) + x_char;
                        putChar(x_char);
                        break;
                    default:
                        this.ch = chLocal;
                        throw new JSONException("unclosed.str.lit");
                }
            } else {
                hash = (hash * 31) + chLocal;
                if (hasSpecial) {
                    if (this.sp == this.sbuf.length) {
                        putChar(chLocal);
                    } else {
                        char[] cArr = this.sbuf;
                        int i = this.sp;
                        this.sp = i + 1;
                        cArr[i] = chLocal;
                    }
                } else {
                    this.sp++;
                }
            }
        }
    }

    public final void resetStringPosition() {
        this.sp = 0;
    }

    public String info() {
        return "";
    }

    public final String scanSymbolUnQuoted(SymbolTable symbolTable) {
        boolean[] firstIdentifierFlags = IOUtils.firstIdentifierFlags;
        char first = this.ch;
        boolean firstFlag = this.ch >= firstIdentifierFlags.length || firstIdentifierFlags[first];
        if (firstFlag) {
            boolean[] identifierFlags = IOUtils.identifierFlags;
            int hash = first;
            this.np = this.bp;
            this.sp = 1;
            while (true) {
                char chLocal = next();
                if (chLocal < identifierFlags.length && !identifierFlags[chLocal]) {
                    break;
                }
                hash = (hash * 31) + chLocal;
                this.sp++;
            }
            this.ch = charAt(this.bp);
            this.token = 18;
            if (this.sp == 4 && hash == 3392903 && charAt(this.np) == 'n' && charAt(this.np + 1) == 'u' && charAt(this.np + 2) == 'l' && charAt(this.np + 3) == 'l') {
                return null;
            }
            return addSymbol(this.np, this.sp, hash, symbolTable);
        }
        throw new JSONException("illegal identifier : " + this.ch + info());
    }

    public final void scanString() {
        this.np = this.bp;
        this.hasSpecial = false;
        while (true) {
            char ch = next();
            if (ch == '\"') {
                this.token = 4;
                this.ch = next();
                return;
            } else if (ch == JSONLexer.EOI) {
                if (isEOF()) {
                    throw new JSONException("unclosed string : " + ch);
                }
                putChar(JSONLexer.EOI);
            } else if (ch == '\\') {
                if (!this.hasSpecial) {
                    this.hasSpecial = true;
                    if (this.sp >= this.sbuf.length) {
                        int newCapcity = this.sbuf.length * 2;
                        if (this.sp > newCapcity) {
                            newCapcity = this.sp;
                        }
                        char[] newsbuf = new char[newCapcity];
                        System.arraycopy(this.sbuf, 0, newsbuf, 0, this.sbuf.length);
                        this.sbuf = newsbuf;
                    }
                    copyTo(this.np + 1, this.sp, this.sbuf);
                }
                ch = next();
                switch (ch) {
                    case '\"':
                        putChar('\"');
                        break;
                    case '\'':
                        putChar('\'');
                        break;
                    case '/':
                        putChar('/');
                        break;
                    case '0':
                        putChar('\u0000');
                        break;
                    case '1':
                        putChar('\u0001');
                        break;
                    case '2':
                        putChar('\u0002');
                        break;
                    case '3':
                        putChar('\u0003');
                        break;
                    case '4':
                        putChar('\u0004');
                        break;
                    case '5':
                        putChar('\u0005');
                        break;
                    case '6':
                        putChar('\u0006');
                        break;
                    case '7':
                        putChar('\u0007');
                        break;
                    case UikitEventType.UIKIT_ADD_DETAIL_CARDS /*70*/:
                    case 'f':
                        putChar('\f');
                        break;
                    case '\\':
                        putChar('\\');
                        break;
                    case Service.TACNEWS /*98*/:
                        putChar('\b');
                        break;
                    case 'n':
                        putChar('\n');
                        break;
                    case HistoryInfoHelper.MSG_CACHE_CLEAR_ALL /*114*/:
                        putChar('\r');
                        break;
                    case 't':
                        putChar('\t');
                        break;
                    case Service.UUCP_PATH /*117*/:
                        char u1 = next();
                        char u2 = next();
                        char u3 = next();
                        char u4 = next();
                        putChar((char) Integer.parseInt(new String(new char[]{u1, u2, u3, u4}), 16));
                        break;
                    case ItemSize.ITEM_118 /*118*/:
                        putChar('\u000b');
                        break;
                    case 'x':
                        putChar((char) ((digits[next()] * 16) + digits[next()]));
                        break;
                    default:
                        this.ch = ch;
                        throw new JSONException("unclosed string : " + ch);
                }
            } else if (!this.hasSpecial) {
                this.sp++;
            } else if (this.sp == this.sbuf.length) {
                putChar(ch);
            } else {
                char[] cArr = this.sbuf;
                int i = this.sp;
                this.sp = i + 1;
                cArr[i] = ch;
            }
        }
    }

    public Calendar getCalendar() {
        return this.calendar;
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public final int intValue() {
        int limit;
        int i;
        if (this.np == -1) {
            this.np = 0;
        }
        int result = 0;
        boolean negative = false;
        int i2 = this.np;
        int max = this.np + this.sp;
        if (charAt(this.np) == '-') {
            negative = true;
            limit = Integer.MIN_VALUE;
            i = i2 + 1;
        } else {
            limit = -2147483647;
            i = i2;
        }
        if (i < max) {
            result = -(charAt(i) - 48);
            i++;
        }
        while (i < max) {
            i2 = i + 1;
            char chLocal = charAt(i);
            if (chLocal == 'L' || chLocal == 'S' || chLocal == 'B') {
                break;
            }
            int digit = chLocal - 48;
            if (((long) result) < -214748364) {
                throw new NumberFormatException(numberString());
            }
            result *= 10;
            if (result < limit + digit) {
                throw new NumberFormatException(numberString());
            }
            result -= digit;
            i = i2;
        }
        i2 = i;
        if (!negative) {
            return -result;
        }
        if (i2 > this.np + 1) {
            return result;
        }
        throw new NumberFormatException(numberString());
    }

    public void close() {
        if (this.sbuf.length <= 8192) {
            SBUF_LOCAL.set(this.sbuf);
        }
        this.sbuf = null;
    }

    public final boolean isRef() {
        if (this.sp == 4 && charAt(this.np + 1) == '$' && charAt(this.np + 2) == 'r' && charAt(this.np + 3) == 'e' && charAt(this.np + 4) == 'f') {
            return true;
        }
        return false;
    }

    public final int scanType(String type) {
        this.matchStat = 0;
        if (!charArrayCompare(typeFieldName)) {
            return -2;
        }
        int bpLocal = this.bp + typeFieldName.length;
        int typeLength = type.length();
        for (int i = 0; i < typeLength; i++) {
            if (type.charAt(i) != charAt(bpLocal + i)) {
                return -1;
            }
        }
        bpLocal += typeLength;
        if (charAt(bpLocal) != '\"') {
            return -1;
        }
        bpLocal++;
        this.ch = charAt(bpLocal);
        if (this.ch == ',') {
            bpLocal++;
            this.ch = charAt(bpLocal);
            this.bp = bpLocal;
            this.token = 16;
            return 3;
        }
        if (this.ch == '}') {
            bpLocal++;
            this.ch = charAt(bpLocal);
            if (this.ch == ',') {
                this.token = 16;
                bpLocal++;
                this.ch = charAt(bpLocal);
            } else if (this.ch == ']') {
                this.token = 15;
                bpLocal++;
                this.ch = charAt(bpLocal);
            } else if (this.ch == '}') {
                this.token = 13;
                bpLocal++;
                this.ch = charAt(bpLocal);
            } else if (this.ch != JSONLexer.EOI) {
                return -1;
            } else {
                this.token = 20;
            }
            this.matchStat = 4;
        }
        this.bp = bpLocal;
        return this.matchStat;
    }

    public final boolean matchField(char[] fieldName) {
        if (!charArrayCompare(fieldName)) {
            return false;
        }
        this.bp += fieldName.length;
        this.ch = charAt(this.bp);
        if (this.ch == '{') {
            next();
            this.token = 12;
        } else if (this.ch == '[') {
            next();
            this.token = 14;
        } else if (this.ch == 'S' && charAt(this.bp + 1) == 'e' && charAt(this.bp + 2) == 't' && charAt(this.bp + 3) == '[') {
            this.bp += 3;
            this.ch = charAt(this.bp);
            this.token = 21;
        } else {
            nextToken();
        }
        return true;
    }

    public String scanFieldString(char[] fieldName) {
        this.matchStat = 0;
        if (charArrayCompare(fieldName)) {
            int offset = fieldName.length;
            int offset2 = offset + 1;
            if (charAt(this.bp + offset) != '\"') {
                this.matchStat = -1;
                return stringDefaultValue();
            }
            int endIndex = indexOf('\"', (this.bp + fieldName.length) + 1);
            if (endIndex == -1) {
                throw new JSONException("unclosed str");
            }
            int startIndex2 = (this.bp + fieldName.length) + 1;
            String stringVal = subString(startIndex2, endIndex - startIndex2);
            if (stringVal.indexOf(92) != -1) {
                while (true) {
                    int slashCount = 0;
                    int i = endIndex - 1;
                    while (i >= 0 && charAt(i) == '\\') {
                        slashCount++;
                        i--;
                    }
                    if (slashCount % 2 == 0) {
                        break;
                    }
                    endIndex = indexOf('\"', endIndex + 1);
                }
                int chars_len = endIndex - ((this.bp + fieldName.length) + 1);
                stringVal = readString(sub_chars((this.bp + fieldName.length) + 1, chars_len), chars_len);
            }
            offset = offset2 + ((endIndex - ((this.bp + fieldName.length) + 1)) + 1);
            offset2 = offset + 1;
            char chLocal = charAt(this.bp + offset);
            String strVal = stringVal;
            if (chLocal == ',') {
                this.bp += offset2;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                return strVal;
            } else if (chLocal == '}') {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
                if (chLocal == ',') {
                    this.token = 16;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == ']') {
                    this.token = 15;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == '}') {
                    this.token = 13;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == JSONLexer.EOI) {
                    this.token = 20;
                    this.bp += offset - 1;
                    this.ch = JSONLexer.EOI;
                } else {
                    this.matchStat = -1;
                    return stringDefaultValue();
                }
                this.matchStat = 4;
                return strVal;
            } else {
                this.matchStat = -1;
                return stringDefaultValue();
            }
        }
        this.matchStat = -2;
        return stringDefaultValue();
    }

    public String scanString(char expectNextChar) {
        this.matchStat = 0;
        int offset = 0 + 1;
        char chLocal = charAt(this.bp + 0);
        int offset2;
        if (chLocal == 'n') {
            if (charAt(this.bp + 1) == 'u' && charAt((this.bp + 1) + 1) == 'l' && charAt((this.bp + 1) + 2) == 'l') {
                offset2 = (offset + 3) + 1;
                if (charAt(this.bp + 4) == expectNextChar) {
                    this.bp += 5;
                    this.ch = charAt(this.bp);
                    this.matchStat = 3;
                    return null;
                }
                this.matchStat = -1;
                return null;
            }
            this.matchStat = -1;
            offset2 = offset;
            return null;
        } else if (chLocal != '\"') {
            this.matchStat = -1;
            offset2 = offset;
            return stringDefaultValue();
        } else {
            int startIndex = this.bp + 1;
            int endIndex = indexOf('\"', startIndex);
            if (endIndex == -1) {
                throw new JSONException("unclosed str");
            }
            String stringVal = subString(this.bp + 1, endIndex - startIndex);
            if (stringVal.indexOf(92) != -1) {
                while (true) {
                    int slashCount = 0;
                    int i = endIndex - 1;
                    while (i >= 0 && charAt(i) == '\\') {
                        slashCount++;
                        i--;
                    }
                    if (slashCount % 2 == 0) {
                        break;
                    }
                    endIndex = indexOf('\"', endIndex + 1);
                }
                int chars_len = endIndex - startIndex;
                stringVal = readString(sub_chars(this.bp + 1, chars_len), chars_len);
            }
            offset2 = ((endIndex - (this.bp + 1)) + 1) + 1;
            offset = offset2 + 1;
            String strVal = stringVal;
            if (charAt(this.bp + offset2) == expectNextChar) {
                this.bp += offset;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                offset2 = offset;
                return strVal;
            }
            this.matchStat = -1;
            offset2 = offset;
            return strVal;
        }
    }

    public String scanFieldSymbol(char[] fieldName, SymbolTable symbolTable) {
        this.matchStat = 0;
        if (charArrayCompare(fieldName)) {
            int offset = fieldName.length;
            int offset2 = offset + 1;
            if (charAt(this.bp + offset) != '\"') {
                this.matchStat = -1;
                return null;
            }
            char chLocal;
            int hash = 0;
            offset = offset2;
            while (true) {
                offset2 = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal == '\"') {
                    break;
                }
                hash = (hash * 31) + chLocal;
                if (chLocal == '\\') {
                    this.matchStat = -1;
                    return null;
                }
                offset = offset2;
            }
            int start = (this.bp + fieldName.length) + 1;
            String strVal = addSymbol(start, ((this.bp + offset2) - start) - 1, hash, symbolTable);
            offset = offset2 + 1;
            chLocal = charAt(this.bp + offset2);
            if (chLocal == ',') {
                this.bp += offset;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                return strVal;
            } else if (chLocal == '}') {
                offset2 = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal == ',') {
                    this.token = 16;
                    this.bp += offset2;
                    this.ch = charAt(this.bp);
                } else if (chLocal == ']') {
                    this.token = 15;
                    this.bp += offset2;
                    this.ch = charAt(this.bp);
                } else if (chLocal == '}') {
                    this.token = 13;
                    this.bp += offset2;
                    this.ch = charAt(this.bp);
                } else if (chLocal == JSONLexer.EOI) {
                    this.token = 20;
                    this.bp += offset2 - 1;
                    this.ch = JSONLexer.EOI;
                } else {
                    this.matchStat = -1;
                    return null;
                }
                this.matchStat = 4;
                return strVal;
            } else {
                this.matchStat = -1;
                return null;
            }
        }
        this.matchStat = -2;
        return null;
    }

    public Enum<?> scanEnum(Class<?> enumClass, SymbolTable symbolTable, char serperator) {
        String name = scanSymbolWithSeperator(symbolTable, serperator);
        if (name == null) {
            return null;
        }
        return Enum.valueOf(enumClass, name);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.String scanSymbolWithSeperator(com.alibaba.fastjson.parser.SymbolTable r13, char r14) {
        /*
        r12 = this;
        r11 = 108; // 0x6c float:1.51E-43 double:5.34E-322;
        r8 = 34;
        r10 = 3;
        r6 = 0;
        r9 = -1;
        r7 = 0;
        r12.matchStat = r7;
        r3 = 0;
        r7 = r12.bp;
        r4 = r3 + 1;
        r7 = r7 + r3;
        r0 = r12.charAt(r7);
        r7 = 110; // 0x6e float:1.54E-43 double:5.43E-322;
        if (r0 != r7) goto L_0x0062;
    L_0x0018:
        r7 = r12.bp;
        r7 = r7 + 1;
        r7 = r12.charAt(r7);
        r8 = 117; // 0x75 float:1.64E-43 double:5.8E-322;
        if (r7 != r8) goto L_0x005b;
    L_0x0024:
        r7 = r12.bp;
        r7 = r7 + 1;
        r7 = r7 + 1;
        r7 = r12.charAt(r7);
        if (r7 != r11) goto L_0x005b;
    L_0x0030:
        r7 = r12.bp;
        r7 = r7 + 1;
        r7 = r7 + 2;
        r7 = r12.charAt(r7);
        if (r7 != r11) goto L_0x005b;
    L_0x003c:
        r3 = r4 + 3;
        r7 = r12.bp;
        r3 = r3 + 1;
        r7 = r7 + 4;
        r0 = r12.charAt(r7);
        if (r0 != r14) goto L_0x005f;
    L_0x004a:
        r7 = r12.bp;
        r7 = r7 + 5;
        r12.bp = r7;
        r7 = r12.bp;
        r7 = r12.charAt(r7);
        r12.ch = r7;
        r12.matchStat = r10;
    L_0x005a:
        return r6;
    L_0x005b:
        r12.matchStat = r9;
        r3 = r4;
        goto L_0x005a;
    L_0x005f:
        r12.matchStat = r9;
        goto L_0x005a;
    L_0x0062:
        if (r0 == r8) goto L_0x0068;
    L_0x0064:
        r12.matchStat = r9;
        r3 = r4;
        goto L_0x005a;
    L_0x0068:
        r1 = 0;
        r3 = r4;
    L_0x006a:
        r7 = r12.bp;
        r4 = r3 + 1;
        r7 = r7 + r3;
        r0 = r12.charAt(r7);
        if (r0 != r8) goto L_0x00a2;
    L_0x0075:
        r7 = r12.bp;
        r7 = r7 + 0;
        r5 = r7 + 1;
        r7 = r12.bp;
        r7 = r7 + r4;
        r7 = r7 - r5;
        r2 = r7 + -1;
        r6 = r12.addSymbol(r5, r2, r1, r13);
        r7 = r12.bp;
        r3 = r4 + 1;
        r7 = r7 + r4;
        r0 = r12.charAt(r7);
        r4 = r3;
    L_0x008f:
        if (r0 != r14) goto L_0x00ae;
    L_0x0091:
        r7 = r12.bp;
        r7 = r7 + r4;
        r12.bp = r7;
        r7 = r12.bp;
        r7 = r12.charAt(r7);
        r12.ch = r7;
        r12.matchStat = r10;
        r3 = r4;
        goto L_0x005a;
    L_0x00a2:
        r7 = r1 * 31;
        r1 = r7 + r0;
        r7 = 92;
        if (r0 != r7) goto L_0x00c3;
    L_0x00aa:
        r12.matchStat = r9;
        r3 = r4;
        goto L_0x005a;
    L_0x00ae:
        r7 = isWhitespace(r0);
        if (r7 == 0) goto L_0x00bf;
    L_0x00b4:
        r7 = r12.bp;
        r3 = r4 + 1;
        r7 = r7 + r4;
        r0 = r12.charAt(r7);
        r4 = r3;
        goto L_0x008f;
    L_0x00bf:
        r12.matchStat = r9;
        r3 = r4;
        goto L_0x005a;
    L_0x00c3:
        r3 = r4;
        goto L_0x006a;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.alibaba.fastjson.parser.JSONLexerBase.scanSymbolWithSeperator(com.alibaba.fastjson.parser.SymbolTable, char):java.lang.String");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.util.Collection<java.lang.String> scanFieldStringArray(char[] r17, java.lang.Class<?> r18) {
        /*
        r16 = this;
        r14 = 0;
        r0 = r16;
        r0.matchStat = r14;
        r14 = r16.charArrayCompare(r17);
        if (r14 != 0) goto L_0x0012;
    L_0x000b:
        r14 = -2;
        r0 = r16;
        r0.matchStat = r14;
        r7 = 0;
    L_0x0011:
        return r7;
    L_0x0012:
        r14 = java.util.HashSet.class;
        r0 = r18;
        r14 = r0.isAssignableFrom(r14);
        if (r14 == 0) goto L_0x003c;
    L_0x001c:
        r7 = new java.util.HashSet;
        r7.<init>();
    L_0x0021:
        r0 = r17;
        r8 = r0.length;
        r0 = r16;
        r14 = r0.bp;
        r9 = r8 + 1;
        r14 = r14 + r8;
        r0 = r16;
        r1 = r0.charAt(r14);
        r14 = 91;
        if (r1 == r14) goto L_0x005e;
    L_0x0035:
        r14 = -1;
        r0 = r16;
        r0.matchStat = r14;
        r7 = 0;
        goto L_0x0011;
    L_0x003c:
        r14 = java.util.ArrayList.class;
        r0 = r18;
        r14 = r0.isAssignableFrom(r14);
        if (r14 == 0) goto L_0x004c;
    L_0x0046:
        r7 = new java.util.ArrayList;
        r7.<init>();
        goto L_0x0021;
    L_0x004c:
        r7 = r18.newInstance();	 Catch:{ Exception -> 0x0053 }
        r7 = (java.util.Collection) r7;	 Catch:{ Exception -> 0x0053 }
        goto L_0x0021;
    L_0x0053:
        r4 = move-exception;
        r14 = new com.alibaba.fastjson.JSONException;
        r15 = r4.getMessage();
        r14.<init>(r15, r4);
        throw r14;
    L_0x005e:
        r0 = r16;
        r14 = r0.bp;
        r8 = r9 + 1;
        r14 = r14 + r9;
        r0 = r16;
        r1 = r0.charAt(r14);
        r9 = r8;
    L_0x006c:
        r14 = 34;
        if (r1 != r14) goto L_0x0109;
    L_0x0070:
        r0 = r16;
        r14 = r0.bp;
        r11 = r14 + r9;
        r14 = 34;
        r0 = r16;
        r5 = r0.indexOf(r14, r11);
        r14 = -1;
        if (r5 != r14) goto L_0x008a;
    L_0x0081:
        r14 = new com.alibaba.fastjson.JSONException;
        r15 = "unclosed str";
        r14.<init>(r15);
        throw r14;
    L_0x008a:
        r0 = r16;
        r14 = r0.bp;
        r12 = r14 + r9;
        r14 = r5 - r12;
        r0 = r16;
        r13 = r0.subString(r12, r14);
        r14 = 92;
        r14 = r13.indexOf(r14);
        r15 = -1;
        if (r14 == r15) goto L_0x00cf;
    L_0x00a1:
        r10 = 0;
        r6 = r5 + -1;
    L_0x00a4:
        if (r6 < 0) goto L_0x00b5;
    L_0x00a6:
        r0 = r16;
        r14 = r0.charAt(r6);
        r15 = 92;
        if (r14 != r15) goto L_0x00b5;
    L_0x00b0:
        r10 = r10 + 1;
        r6 = r6 + -1;
        goto L_0x00a4;
    L_0x00b5:
        r14 = r10 % 2;
        if (r14 != 0) goto L_0x00fe;
    L_0x00b9:
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r9;
        r3 = r5 - r14;
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r9;
        r0 = r16;
        r2 = r0.sub_chars(r14, r3);
        r13 = readString(r2, r3);
    L_0x00cf:
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r9;
        r14 = r5 - r14;
        r14 = r14 + 1;
        r8 = r9 + r14;
        r0 = r16;
        r14 = r0.bp;
        r9 = r8 + 1;
        r14 = r14 + r8;
        r0 = r16;
        r1 = r0.charAt(r14);
        r7.add(r13);
    L_0x00ea:
        r14 = 44;
        if (r1 != r14) goto L_0x0195;
    L_0x00ee:
        r0 = r16;
        r14 = r0.bp;
        r8 = r9 + 1;
        r14 = r14 + r9;
        r0 = r16;
        r1 = r0.charAt(r14);
        r9 = r8;
        goto L_0x006c;
    L_0x00fe:
        r14 = 34;
        r15 = r5 + 1;
        r0 = r16;
        r5 = r0.indexOf(r14, r15);
        goto L_0x00a1;
    L_0x0109:
        r14 = 110; // 0x6e float:1.54E-43 double:5.43E-322;
        if (r1 != r14) goto L_0x0152;
    L_0x010d:
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r9;
        r0 = r16;
        r14 = r0.charAt(r14);
        r15 = 117; // 0x75 float:1.64E-43 double:5.8E-322;
        if (r14 != r15) goto L_0x0152;
    L_0x011c:
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r9;
        r14 = r14 + 1;
        r0 = r16;
        r14 = r0.charAt(r14);
        r15 = 108; // 0x6c float:1.51E-43 double:5.34E-322;
        if (r14 != r15) goto L_0x0152;
    L_0x012d:
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r9;
        r14 = r14 + 2;
        r0 = r16;
        r14 = r0.charAt(r14);
        r15 = 108; // 0x6c float:1.51E-43 double:5.34E-322;
        if (r14 != r15) goto L_0x0152;
    L_0x013e:
        r8 = r9 + 3;
        r0 = r16;
        r14 = r0.bp;
        r9 = r8 + 1;
        r14 = r14 + r8;
        r0 = r16;
        r1 = r0.charAt(r14);
        r14 = 0;
        r7.add(r14);
        goto L_0x00ea;
    L_0x0152:
        r14 = 93;
        if (r1 != r14) goto L_0x018c;
    L_0x0156:
        r14 = r7.size();
        if (r14 != 0) goto L_0x018c;
    L_0x015c:
        r0 = r16;
        r14 = r0.bp;
        r8 = r9 + 1;
        r14 = r14 + r9;
        r0 = r16;
        r1 = r0.charAt(r14);
        r9 = r8;
    L_0x016a:
        r14 = 44;
        if (r1 != r14) goto L_0x01b0;
    L_0x016e:
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r9;
        r0 = r16;
        r0.bp = r14;
        r0 = r16;
        r14 = r0.bp;
        r0 = r16;
        r14 = r0.charAt(r14);
        r0 = r16;
        r0.ch = r14;
        r14 = 3;
        r0 = r16;
        r0.matchStat = r14;
        goto L_0x0011;
    L_0x018c:
        r14 = new com.alibaba.fastjson.JSONException;
        r15 = "illega str";
        r14.<init>(r15);
        throw r14;
    L_0x0195:
        r14 = 93;
        if (r1 != r14) goto L_0x01a8;
    L_0x0199:
        r0 = r16;
        r14 = r0.bp;
        r8 = r9 + 1;
        r14 = r14 + r9;
        r0 = r16;
        r1 = r0.charAt(r14);
        r9 = r8;
        goto L_0x016a;
    L_0x01a8:
        r14 = -1;
        r0 = r16;
        r0.matchStat = r14;
        r7 = 0;
        goto L_0x0011;
    L_0x01b0:
        r14 = 125; // 0x7d float:1.75E-43 double:6.2E-322;
        if (r1 != r14) goto L_0x0251;
    L_0x01b4:
        r0 = r16;
        r14 = r0.bp;
        r8 = r9 + 1;
        r14 = r14 + r9;
        r0 = r16;
        r1 = r0.charAt(r14);
        r14 = 44;
        if (r1 != r14) goto L_0x01e9;
    L_0x01c5:
        r14 = 16;
        r0 = r16;
        r0.token = r14;
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r8;
        r0 = r16;
        r0.bp = r14;
        r0 = r16;
        r14 = r0.bp;
        r0 = r16;
        r14 = r0.charAt(r14);
        r0 = r16;
        r0.ch = r14;
    L_0x01e2:
        r14 = 4;
        r0 = r16;
        r0.matchStat = r14;
        goto L_0x0011;
    L_0x01e9:
        r14 = 93;
        if (r1 != r14) goto L_0x020b;
    L_0x01ed:
        r14 = 15;
        r0 = r16;
        r0.token = r14;
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r8;
        r0 = r16;
        r0.bp = r14;
        r0 = r16;
        r14 = r0.bp;
        r0 = r16;
        r14 = r0.charAt(r14);
        r0 = r16;
        r0.ch = r14;
        goto L_0x01e2;
    L_0x020b:
        r14 = 125; // 0x7d float:1.75E-43 double:6.2E-322;
        if (r1 != r14) goto L_0x022d;
    L_0x020f:
        r14 = 13;
        r0 = r16;
        r0.token = r14;
        r0 = r16;
        r14 = r0.bp;
        r14 = r14 + r8;
        r0 = r16;
        r0.bp = r14;
        r0 = r16;
        r14 = r0.bp;
        r0 = r16;
        r14 = r0.charAt(r14);
        r0 = r16;
        r0.ch = r14;
        goto L_0x01e2;
    L_0x022d:
        r14 = 26;
        if (r1 != r14) goto L_0x0249;
    L_0x0231:
        r0 = r16;
        r14 = r0.bp;
        r15 = r8 + -1;
        r14 = r14 + r15;
        r0 = r16;
        r0.bp = r14;
        r14 = 20;
        r0 = r16;
        r0.token = r14;
        r14 = 26;
        r0 = r16;
        r0.ch = r14;
        goto L_0x01e2;
    L_0x0249:
        r14 = -1;
        r0 = r16;
        r0.matchStat = r14;
        r7 = 0;
        goto L_0x0011;
    L_0x0251:
        r14 = -1;
        r0 = r16;
        r0.matchStat = r14;
        r7 = 0;
        goto L_0x0011;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.alibaba.fastjson.parser.JSONLexerBase.scanFieldStringArray(char[], java.lang.Class):java.util.Collection<java.lang.String>");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void scanStringArray(java.util.Collection<java.lang.String> r13, char r14) {
        /*
        r12 = this;
        r10 = 0;
        r12.matchStat = r10;
        r5 = 0;
        r10 = r12.bp;
        r6 = r5 + 1;
        r10 = r10 + r5;
        r0 = r12.charAt(r10);
        r10 = 110; // 0x6e float:1.54E-43 double:5.43E-322;
        if (r0 != r10) goto L_0x0058;
    L_0x0011:
        r10 = r12.bp;
        r10 = r10 + 1;
        r10 = r12.charAt(r10);
        r11 = 117; // 0x75 float:1.64E-43 double:5.8E-322;
        if (r10 != r11) goto L_0x0058;
    L_0x001d:
        r10 = r12.bp;
        r10 = r10 + 1;
        r10 = r10 + 1;
        r10 = r12.charAt(r10);
        r11 = 108; // 0x6c float:1.51E-43 double:5.34E-322;
        if (r10 != r11) goto L_0x0058;
    L_0x002b:
        r10 = r12.bp;
        r10 = r10 + 1;
        r10 = r10 + 2;
        r10 = r12.charAt(r10);
        r11 = 108; // 0x6c float:1.51E-43 double:5.34E-322;
        if (r10 != r11) goto L_0x0058;
    L_0x0039:
        r10 = r12.bp;
        r10 = r10 + 1;
        r10 = r10 + 3;
        r10 = r12.charAt(r10);
        if (r10 != r14) goto L_0x0058;
    L_0x0045:
        r10 = r12.bp;
        r10 = r10 + 5;
        r12.bp = r10;
        r10 = r12.bp;
        r10 = r12.charAt(r10);
        r12.ch = r10;
        r10 = 5;
        r12.matchStat = r10;
        r5 = r6;
    L_0x0057:
        return;
    L_0x0058:
        r10 = 91;
        if (r0 == r10) goto L_0x0061;
    L_0x005c:
        r10 = -1;
        r12.matchStat = r10;
        r5 = r6;
        goto L_0x0057;
    L_0x0061:
        r10 = r12.bp;
        r5 = r6 + 1;
        r10 = r10 + 1;
        r0 = r12.charAt(r10);
        r6 = r5;
    L_0x006c:
        r10 = 110; // 0x6e float:1.54E-43 double:5.43E-322;
        if (r0 != r10) goto L_0x00b3;
    L_0x0070:
        r10 = r12.bp;
        r10 = r10 + r6;
        r10 = r12.charAt(r10);
        r11 = 117; // 0x75 float:1.64E-43 double:5.8E-322;
        if (r10 != r11) goto L_0x00b3;
    L_0x007b:
        r10 = r12.bp;
        r10 = r10 + r6;
        r10 = r10 + 1;
        r10 = r12.charAt(r10);
        r11 = 108; // 0x6c float:1.51E-43 double:5.34E-322;
        if (r10 != r11) goto L_0x00b3;
    L_0x0088:
        r10 = r12.bp;
        r10 = r10 + r6;
        r10 = r10 + 2;
        r10 = r12.charAt(r10);
        r11 = 108; // 0x6c float:1.51E-43 double:5.34E-322;
        if (r10 != r11) goto L_0x00b3;
    L_0x0095:
        r5 = r6 + 3;
        r10 = r12.bp;
        r6 = r5 + 1;
        r10 = r10 + r5;
        r0 = r12.charAt(r10);
        r10 = 0;
        r13.add(r10);
    L_0x00a4:
        r10 = 44;
        if (r0 != r10) goto L_0x014f;
    L_0x00a8:
        r10 = r12.bp;
        r5 = r6 + 1;
        r10 = r10 + r6;
        r0 = r12.charAt(r10);
        r6 = r5;
        goto L_0x006c;
    L_0x00b3:
        r10 = 93;
        if (r0 != r10) goto L_0x00da;
    L_0x00b7:
        r10 = r13.size();
        if (r10 != 0) goto L_0x00da;
    L_0x00bd:
        r10 = r12.bp;
        r5 = r6 + 1;
        r10 = r10 + r6;
        r0 = r12.charAt(r10);
    L_0x00c6:
        if (r0 != r14) goto L_0x0164;
    L_0x00c8:
        r10 = r12.bp;
        r10 = r10 + r5;
        r12.bp = r10;
        r10 = r12.bp;
        r10 = r12.charAt(r10);
        r12.ch = r10;
        r10 = 3;
        r12.matchStat = r10;
        goto L_0x0057;
    L_0x00da:
        r10 = 34;
        if (r0 == r10) goto L_0x00e4;
    L_0x00de:
        r10 = -1;
        r12.matchStat = r10;
        r5 = r6;
        goto L_0x0057;
    L_0x00e4:
        r10 = r12.bp;
        r8 = r10 + r6;
        r10 = 34;
        r3 = r12.indexOf(r10, r8);
        r10 = -1;
        if (r3 != r10) goto L_0x00fa;
    L_0x00f1:
        r10 = new com.alibaba.fastjson.JSONException;
        r11 = "unclosed str";
        r10.<init>(r11);
        throw r10;
    L_0x00fa:
        r10 = r12.bp;
        r10 = r10 + r6;
        r11 = r3 - r8;
        r9 = r12.subString(r10, r11);
        r10 = 92;
        r10 = r9.indexOf(r10);
        r11 = -1;
        if (r10 == r11) goto L_0x012f;
    L_0x010c:
        r7 = 0;
        r4 = r3 + -1;
    L_0x010f:
        if (r4 < 0) goto L_0x011e;
    L_0x0111:
        r10 = r12.charAt(r4);
        r11 = 92;
        if (r10 != r11) goto L_0x011e;
    L_0x0119:
        r7 = r7 + 1;
        r4 = r4 + -1;
        goto L_0x010f;
    L_0x011e:
        r10 = r7 % 2;
        if (r10 != 0) goto L_0x0146;
    L_0x0122:
        r2 = r3 - r8;
        r10 = r12.bp;
        r10 = r10 + r6;
        r1 = r12.sub_chars(r10, r2);
        r9 = readString(r1, r2);
    L_0x012f:
        r10 = r12.bp;
        r10 = r10 + r6;
        r10 = r3 - r10;
        r10 = r10 + 1;
        r5 = r6 + r10;
        r10 = r12.bp;
        r6 = r5 + 1;
        r10 = r10 + r5;
        r0 = r12.charAt(r10);
        r13.add(r9);
        goto L_0x00a4;
    L_0x0146:
        r10 = 34;
        r11 = r3 + 1;
        r3 = r12.indexOf(r10, r11);
        goto L_0x010c;
    L_0x014f:
        r10 = 93;
        if (r0 != r10) goto L_0x015e;
    L_0x0153:
        r10 = r12.bp;
        r5 = r6 + 1;
        r10 = r10 + r6;
        r0 = r12.charAt(r10);
        goto L_0x00c6;
    L_0x015e:
        r10 = -1;
        r12.matchStat = r10;
        r5 = r6;
        goto L_0x0057;
    L_0x0164:
        r10 = -1;
        r12.matchStat = r10;
        goto L_0x0057;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.alibaba.fastjson.parser.JSONLexerBase.scanStringArray(java.util.Collection, char):void");
    }

    public int scanFieldInt(char[] fieldName) {
        this.matchStat = 0;
        if (charArrayCompare(fieldName)) {
            boolean negative;
            int offset = fieldName.length;
            int offset2 = offset + 1;
            char chLocal = charAt(this.bp + offset);
            if (chLocal == '-') {
                negative = true;
            } else {
                negative = false;
            }
            if (negative) {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
            } else {
                offset = offset2;
            }
            if (chLocal < '0' || chLocal > '9') {
                this.matchStat = -1;
                return 0;
            }
            int value = chLocal - 48;
            while (true) {
                offset2 = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal >= '0' && chLocal <= '9') {
                    value = (value * 10) + (chLocal - 48);
                    offset = offset2;
                }
            }
            if (chLocal == '.') {
                this.matchStat = -1;
                return 0;
            } else if ((value < 0 || offset2 > fieldName.length + 14) && !(value == Integer.MIN_VALUE && offset2 == 17 && negative)) {
                this.matchStat = -1;
                return 0;
            } else if (chLocal == ',') {
                this.bp += offset2;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                this.token = 16;
                if (negative) {
                    return -value;
                }
                return value;
            } else if (chLocal == '}') {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
                if (chLocal == ',') {
                    this.token = 16;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == ']') {
                    this.token = 15;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == '}') {
                    this.token = 13;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == JSONLexer.EOI) {
                    this.token = 20;
                    this.bp += offset - 1;
                    this.ch = JSONLexer.EOI;
                } else {
                    this.matchStat = -1;
                    return 0;
                }
                this.matchStat = 4;
                if (negative) {
                    return -value;
                }
                return value;
            } else {
                this.matchStat = -1;
                return 0;
            }
        }
        this.matchStat = -2;
        return 0;
    }

    public boolean scanBoolean(char expectNext) {
        int offset;
        this.matchStat = 0;
        int offset2 = 0 + 1;
        char chLocal = charAt(this.bp + 0);
        boolean value = false;
        if (chLocal == 't') {
            if (charAt(this.bp + 1) == 'r' && charAt((this.bp + 1) + 1) == 'u' && charAt((this.bp + 1) + 2) == 'e') {
                offset = (offset2 + 3) + 1;
                chLocal = charAt(this.bp + 4);
                value = true;
                offset2 = offset;
            } else {
                this.matchStat = -1;
                offset = offset2;
                return false;
            }
        } else if (chLocal == 'f') {
            if (charAt(this.bp + 1) == 'a' && charAt((this.bp + 1) + 1) == 'l' && charAt((this.bp + 1) + 2) == 's' && charAt((this.bp + 1) + 3) == 'e') {
                offset = (offset2 + 4) + 1;
                chLocal = charAt(this.bp + 5);
                value = false;
                offset2 = offset;
            } else {
                this.matchStat = -1;
                offset = offset2;
                return false;
            }
        } else if (chLocal == '1') {
            offset = offset2 + 1;
            chLocal = charAt(this.bp + 1);
            value = true;
            offset2 = offset;
        } else if (chLocal == '0') {
            offset = offset2 + 1;
            chLocal = charAt(this.bp + 1);
            value = false;
            offset2 = offset;
        }
        while (chLocal != expectNext) {
            if (isWhitespace(chLocal)) {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
                offset2 = offset;
            } else {
                this.matchStat = -1;
                offset = offset2;
                return value;
            }
        }
        this.bp += offset2;
        this.ch = charAt(this.bp);
        this.matchStat = 3;
        offset = offset2;
        return value;
    }

    public int scanInt(char expectNext) {
        boolean negative;
        int offset;
        this.matchStat = 0;
        int offset2 = 0 + 1;
        char chLocal = charAt(this.bp + 0);
        if (chLocal == '-') {
            negative = true;
        } else {
            negative = false;
        }
        if (negative) {
            offset = offset2 + 1;
            chLocal = charAt(this.bp + 1);
        } else {
            offset = offset2;
        }
        if (chLocal < '0' || chLocal > '9') {
            this.matchStat = -1;
            return 0;
        }
        int value = chLocal - 48;
        while (true) {
            offset2 = offset + 1;
            chLocal = charAt(this.bp + offset);
            if (chLocal >= '0' && chLocal <= '9') {
                value = (value * 10) + (chLocal - 48);
                offset = offset2;
            }
        }
        if (chLocal == '.') {
            this.matchStat = -1;
            offset = offset2;
            return 0;
        } else if (value < 0) {
            this.matchStat = -1;
            offset = offset2;
            return 0;
        } else {
            while (chLocal != expectNext) {
                if (isWhitespace(chLocal)) {
                    offset = offset2 + 1;
                    chLocal = charAt(this.bp + offset2);
                    offset2 = offset;
                } else {
                    this.matchStat = -1;
                    if (negative) {
                        value = -value;
                    }
                    offset = offset2;
                    return value;
                }
            }
            this.bp += offset2;
            this.ch = charAt(this.bp);
            this.matchStat = 3;
            this.token = 16;
            if (negative) {
                value = -value;
            }
            offset = offset2;
            return value;
        }
    }

    public boolean scanFieldBoolean(char[] fieldName) {
        this.matchStat = 0;
        if (charArrayCompare(fieldName)) {
            boolean value;
            int offset = fieldName.length;
            int offset2 = offset + 1;
            char chLocal = charAt(this.bp + offset);
            if (chLocal == 't') {
                offset = offset2 + 1;
                if (charAt(this.bp + offset2) != 'r') {
                    this.matchStat = -1;
                    return false;
                }
                offset2 = offset + 1;
                if (charAt(this.bp + offset) != 'u') {
                    this.matchStat = -1;
                    return false;
                }
                offset = offset2 + 1;
                if (charAt(this.bp + offset2) != 'e') {
                    this.matchStat = -1;
                    return false;
                }
                value = true;
            } else if (chLocal == 'f') {
                offset = offset2 + 1;
                if (charAt(this.bp + offset2) != 'a') {
                    this.matchStat = -1;
                    return false;
                }
                offset2 = offset + 1;
                if (charAt(this.bp + offset) != 'l') {
                    this.matchStat = -1;
                    return false;
                }
                offset = offset2 + 1;
                if (charAt(this.bp + offset2) != 's') {
                    this.matchStat = -1;
                    return false;
                }
                offset2 = offset + 1;
                if (charAt(this.bp + offset) != 'e') {
                    this.matchStat = -1;
                    return false;
                }
                value = false;
                offset = offset2;
            } else {
                this.matchStat = -1;
                return false;
            }
            offset2 = offset + 1;
            chLocal = charAt(this.bp + offset);
            if (chLocal == ',') {
                this.bp += offset2;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                this.token = 16;
                return value;
            } else if (chLocal == '}') {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
                if (chLocal == ',') {
                    this.token = 16;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == ']') {
                    this.token = 15;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == '}') {
                    this.token = 13;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == JSONLexer.EOI) {
                    this.token = 20;
                    this.bp += offset - 1;
                    this.ch = JSONLexer.EOI;
                } else {
                    this.matchStat = -1;
                    return false;
                }
                this.matchStat = 4;
                return value;
            } else {
                this.matchStat = -1;
                return false;
            }
        }
        this.matchStat = -2;
        return false;
    }

    public long scanFieldLong(char[] fieldName) {
        this.matchStat = 0;
        if (charArrayCompare(fieldName)) {
            int offset = fieldName.length;
            int offset2 = offset + 1;
            char chLocal = charAt(this.bp + offset);
            boolean negative = false;
            if (chLocal == '-') {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
                negative = true;
            } else {
                offset = offset2;
            }
            if (chLocal < '0' || chLocal > '9') {
                this.matchStat = -1;
                return 0;
            }
            long value = (long) (chLocal - 48);
            while (true) {
                offset2 = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal >= '0' && chLocal <= '9') {
                    value = (10 * value) + ((long) (chLocal - 48));
                    offset = offset2;
                }
            }
            if (chLocal == '.') {
                this.matchStat = -1;
                return 0;
            } else if (value < 0 || offset2 > 21) {
                this.matchStat = -1;
                return 0;
            } else if (chLocal == ',') {
                this.bp += offset2;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                this.token = 16;
                if (negative) {
                    return -value;
                }
                return value;
            } else if (chLocal == '}') {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
                if (chLocal == ',') {
                    this.token = 16;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == ']') {
                    this.token = 15;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == '}') {
                    this.token = 13;
                    this.bp += offset;
                    this.ch = charAt(this.bp);
                } else if (chLocal == JSONLexer.EOI) {
                    this.token = 20;
                    this.bp += offset - 1;
                    this.ch = JSONLexer.EOI;
                } else {
                    this.matchStat = -1;
                    return 0;
                }
                this.matchStat = 4;
                if (negative) {
                    return -value;
                }
                return value;
            } else {
                this.matchStat = -1;
                return 0;
            }
        }
        this.matchStat = -2;
        return 0;
    }

    public long scanLong(char expectNextChar) {
        int offset;
        this.matchStat = 0;
        int offset2 = 0 + 1;
        char chLocal = charAt(this.bp + 0);
        boolean negative = chLocal == '-';
        if (negative) {
            offset = offset2 + 1;
            chLocal = charAt(this.bp + 1);
        } else {
            offset = offset2;
        }
        if (chLocal < '0' || chLocal > '9') {
            this.matchStat = -1;
            return 0;
        }
        long value = (long) (chLocal - 48);
        while (true) {
            offset2 = offset + 1;
            chLocal = charAt(this.bp + offset);
            if (chLocal >= '0' && chLocal <= '9') {
                value = (10 * value) + ((long) (chLocal - 48));
                offset = offset2;
            }
        }
        if (chLocal == '.') {
            this.matchStat = -1;
            offset = offset2;
            return 0;
        } else if (value < 0) {
            this.matchStat = -1;
            offset = offset2;
            return 0;
        } else {
            while (chLocal != expectNextChar) {
                if (isWhitespace(chLocal)) {
                    offset = offset2 + 1;
                    chLocal = charAt(this.bp + offset2);
                    offset2 = offset;
                } else {
                    this.matchStat = -1;
                    offset = offset2;
                    return value;
                }
            }
            this.bp += offset2;
            this.ch = charAt(this.bp);
            this.matchStat = 3;
            this.token = 16;
            if (negative) {
                value = -value;
            }
            offset = offset2;
            return value;
        }
    }

    public final float scanFieldFloat(char[] fieldName) {
        this.matchStat = 0;
        if (charArrayCompare(fieldName)) {
            int offset = fieldName.length;
            int offset2 = offset + 1;
            char chLocal = charAt(this.bp + offset);
            if (chLocal < '0' || chLocal > '9') {
                this.matchStat = -1;
                return 0.0f;
            }
            offset = offset2;
            while (true) {
                offset2 = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal >= '0' && chLocal <= '9') {
                    offset = offset2;
                }
            }
            if (chLocal == '.') {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
                if (chLocal >= '0' && chLocal <= '9') {
                    while (true) {
                        offset2 = offset + 1;
                        chLocal = charAt(this.bp + offset);
                        if (chLocal < '0' || chLocal > '9') {
                            break;
                        }
                        offset = offset2;
                    }
                } else {
                    this.matchStat = -1;
                    return 0.0f;
                }
            }
            offset = offset2;
            int start = this.bp + fieldName.length;
            float value = Float.parseFloat(subString(start, ((this.bp + offset) - start) - 1));
            if (chLocal == ',') {
                this.bp += offset;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                this.token = 16;
                return value;
            } else if (chLocal == '}') {
                offset2 = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal == ',') {
                    this.token = 16;
                    this.bp += offset2;
                    this.ch = charAt(this.bp);
                } else if (chLocal == ']') {
                    this.token = 15;
                    this.bp += offset2;
                    this.ch = charAt(this.bp);
                } else if (chLocal == '}') {
                    this.token = 13;
                    this.bp += offset2;
                    this.ch = charAt(this.bp);
                } else if (chLocal == JSONLexer.EOI) {
                    this.bp += offset2 - 1;
                    this.token = 20;
                    this.ch = JSONLexer.EOI;
                } else {
                    this.matchStat = -1;
                    return 0.0f;
                }
                this.matchStat = 4;
                return value;
            } else {
                this.matchStat = -1;
                return 0.0f;
            }
        }
        this.matchStat = -2;
        return 0.0f;
    }

    public final float scanFloat(char seperator) {
        float f = 0.0f;
        this.matchStat = 0;
        int offset = 0 + 1;
        char chLocal = charAt(this.bp + 0);
        int i;
        if (chLocal < '0' || chLocal > '9') {
            this.matchStat = -1;
            i = offset;
        } else {
            i = offset;
            while (true) {
                offset = i + 1;
                chLocal = charAt(this.bp + i);
                if (chLocal >= '0' && chLocal <= '9') {
                    i = offset;
                }
            }
            if (chLocal == '.') {
                i = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal >= '0' && chLocal <= '9') {
                    while (true) {
                        offset = i + 1;
                        chLocal = charAt(this.bp + i);
                        if (chLocal < '0' || chLocal > '9') {
                            break;
                        }
                        i = offset;
                    }
                } else {
                    this.matchStat = -1;
                }
            }
            i = offset;
            int start = this.bp;
            f = Float.parseFloat(subString(start, ((this.bp + i) - start) - 1));
            if (chLocal == seperator) {
                this.bp += i;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                this.token = 16;
            } else {
                this.matchStat = -1;
            }
        }
        return f;
    }

    public final double scanDouble(char seperator) {
        double d = 0.0d;
        this.matchStat = 0;
        int offset = 0 + 1;
        char chLocal = charAt(this.bp + 0);
        int i;
        if (chLocal < '0' || chLocal > '9') {
            this.matchStat = -1;
            i = offset;
        } else {
            i = offset;
            while (true) {
                offset = i + 1;
                chLocal = charAt(this.bp + i);
                if (chLocal >= '0' && chLocal <= '9') {
                    i = offset;
                }
            }
            if (chLocal == '.') {
                i = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal >= '0' && chLocal <= '9') {
                    while (true) {
                        offset = i + 1;
                        chLocal = charAt(this.bp + i);
                        if (chLocal < '0' || chLocal > '9') {
                            break;
                        }
                        i = offset;
                    }
                } else {
                    this.matchStat = -1;
                }
            }
            i = offset;
            int start = this.bp;
            d = Double.parseDouble(subString(start, ((this.bp + i) - start) - 1));
            if (chLocal == seperator) {
                this.bp += i;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                this.token = 16;
            } else {
                this.matchStat = -1;
            }
        }
        return d;
    }

    public final double scanFieldDouble(char[] fieldName) {
        this.matchStat = 0;
        if (charArrayCompare(fieldName)) {
            int offset = fieldName.length;
            int offset2 = offset + 1;
            char chLocal = charAt(this.bp + offset);
            if (chLocal < '0' || chLocal > '9') {
                this.matchStat = -1;
                return 0.0d;
            }
            offset = offset2;
            while (true) {
                offset2 = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal >= '0' && chLocal <= '9') {
                    offset = offset2;
                }
            }
            if (chLocal == '.') {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
                if (chLocal >= '0' && chLocal <= '9') {
                    while (true) {
                        offset2 = offset + 1;
                        chLocal = charAt(this.bp + offset);
                        if (chLocal < '0' || chLocal > '9') {
                            break;
                        }
                        offset = offset2;
                    }
                } else {
                    this.matchStat = -1;
                    return 0.0d;
                }
            }
            if (chLocal == 'e' || chLocal == 'E') {
                offset = offset2 + 1;
                chLocal = charAt(this.bp + offset2);
                if (chLocal == '+' || chLocal == '-') {
                    offset2 = offset + 1;
                    chLocal = charAt(this.bp + offset);
                } else {
                    offset2 = offset;
                }
                while (chLocal >= '0' && chLocal <= '9') {
                    offset = offset2 + 1;
                    chLocal = charAt(this.bp + offset2);
                    offset2 = offset;
                }
            }
            offset = offset2;
            int start = this.bp + fieldName.length;
            double value = Double.parseDouble(subString(start, ((this.bp + offset) - start) - 1));
            if (chLocal == ',') {
                this.bp += offset;
                this.ch = charAt(this.bp);
                this.matchStat = 3;
                this.token = 16;
                return value;
            } else if (chLocal == '}') {
                offset2 = offset + 1;
                chLocal = charAt(this.bp + offset);
                if (chLocal == ',') {
                    this.token = 16;
                    this.bp += offset2;
                    this.ch = charAt(this.bp);
                } else if (chLocal == ']') {
                    this.token = 15;
                    this.bp += offset2;
                    this.ch = charAt(this.bp);
                } else if (chLocal == '}') {
                    this.token = 13;
                    this.bp += offset2;
                    this.ch = charAt(this.bp);
                } else if (chLocal == JSONLexer.EOI) {
                    this.token = 20;
                    this.bp += offset2 - 1;
                    this.ch = JSONLexer.EOI;
                } else {
                    this.matchStat = -1;
                    return 0.0d;
                }
                this.matchStat = 4;
                return value;
            } else {
                this.matchStat = -1;
                return 0.0d;
            }
        }
        this.matchStat = -2;
        return 0.0d;
    }

    public final void scanTrue() {
        if (this.ch != 't') {
            throw new JSONException("error parse true");
        }
        next();
        if (this.ch != 'r') {
            throw new JSONException("error parse true");
        }
        next();
        if (this.ch != 'u') {
            throw new JSONException("error parse true");
        }
        next();
        if (this.ch != 'e') {
            throw new JSONException("error parse true");
        }
        next();
        if (this.ch == ' ' || this.ch == ',' || this.ch == '}' || this.ch == ']' || this.ch == '\n' || this.ch == '\r' || this.ch == '\t' || this.ch == JSONLexer.EOI || this.ch == '\f' || this.ch == '\b' || this.ch == ':') {
            this.token = 6;
            return;
        }
        throw new JSONException("scan true error");
    }

    public final void scanNullOrNew() {
        if (this.ch != 'n') {
            throw new JSONException("error parse null or new");
        }
        next();
        if (this.ch == 'u') {
            next();
            if (this.ch != 'l') {
                throw new JSONException("error parse null");
            }
            next();
            if (this.ch != 'l') {
                throw new JSONException("error parse null");
            }
            next();
            if (this.ch == ' ' || this.ch == ',' || this.ch == '}' || this.ch == ']' || this.ch == '\n' || this.ch == '\r' || this.ch == '\t' || this.ch == JSONLexer.EOI || this.ch == '\f' || this.ch == '\b') {
                this.token = 8;
                return;
            }
            throw new JSONException("scan null error");
        } else if (this.ch != 'e') {
            throw new JSONException("error parse new");
        } else {
            next();
            if (this.ch != 'w') {
                throw new JSONException("error parse new");
            }
            next();
            if (this.ch == ' ' || this.ch == ',' || this.ch == '}' || this.ch == ']' || this.ch == '\n' || this.ch == '\r' || this.ch == '\t' || this.ch == JSONLexer.EOI || this.ch == '\f' || this.ch == '\b') {
                this.token = 9;
                return;
            }
            throw new JSONException("scan new error");
        }
    }

    public final void scanFalse() {
        if (this.ch != 'f') {
            throw new JSONException("error parse false");
        }
        next();
        if (this.ch != 'a') {
            throw new JSONException("error parse false");
        }
        next();
        if (this.ch != 'l') {
            throw new JSONException("error parse false");
        }
        next();
        if (this.ch != 's') {
            throw new JSONException("error parse false");
        }
        next();
        if (this.ch != 'e') {
            throw new JSONException("error parse false");
        }
        next();
        if (this.ch == ' ' || this.ch == ',' || this.ch == '}' || this.ch == ']' || this.ch == '\n' || this.ch == '\r' || this.ch == '\t' || this.ch == JSONLexer.EOI || this.ch == '\f' || this.ch == '\b' || this.ch == ':') {
            this.token = 7;
            return;
        }
        throw new JSONException("scan false error");
    }

    public final void scanIdent() {
        this.np = this.bp - 1;
        this.hasSpecial = false;
        do {
            this.sp++;
            next();
        } while (Character.isLetterOrDigit(this.ch));
        String ident = stringVal();
        if ("null".equalsIgnoreCase(ident)) {
            this.token = 8;
        } else if (IAlbumConfig.BUY_SOURCE_NEW.equals(ident)) {
            this.token = 9;
        } else if ("true".equals(ident)) {
            this.token = 6;
        } else if ("false".equals(ident)) {
            this.token = 7;
        } else if ("undefined".equals(ident)) {
            this.token = 23;
        } else if ("Set".equals(ident)) {
            this.token = 21;
        } else if ("TreeSet".equals(ident)) {
            this.token = 22;
        } else {
            this.token = 18;
        }
    }

    public static String readString(char[] chars, int chars_len) {
        char[] sbuf = new char[chars_len];
        int i = 0;
        int len = 0;
        while (i < chars_len) {
            int len2;
            char ch = chars[i];
            if (ch != '\\') {
                len2 = len + 1;
                sbuf[len] = ch;
            } else {
                i++;
                switch (chars[i]) {
                    case '\"':
                        len2 = len + 1;
                        sbuf[len] = '\"';
                        break;
                    case '\'':
                        len2 = len + 1;
                        sbuf[len] = '\'';
                        break;
                    case '/':
                        len2 = len + 1;
                        sbuf[len] = '/';
                        break;
                    case '0':
                        len2 = len + 1;
                        sbuf[len] = '\u0000';
                        break;
                    case '1':
                        len2 = len + 1;
                        sbuf[len] = '\u0001';
                        break;
                    case '2':
                        len2 = len + 1;
                        sbuf[len] = '\u0002';
                        break;
                    case '3':
                        len2 = len + 1;
                        sbuf[len] = '\u0003';
                        break;
                    case '4':
                        len2 = len + 1;
                        sbuf[len] = '\u0004';
                        break;
                    case '5':
                        len2 = len + 1;
                        sbuf[len] = '\u0005';
                        break;
                    case '6':
                        len2 = len + 1;
                        sbuf[len] = '\u0006';
                        break;
                    case '7':
                        len2 = len + 1;
                        sbuf[len] = '\u0007';
                        break;
                    case UikitEventType.UIKIT_ADD_DETAIL_CARDS /*70*/:
                    case 'f':
                        len2 = len + 1;
                        sbuf[len] = '\f';
                        break;
                    case '\\':
                        len2 = len + 1;
                        sbuf[len] = '\\';
                        break;
                    case Service.TACNEWS /*98*/:
                        len2 = len + 1;
                        sbuf[len] = '\b';
                        break;
                    case 'n':
                        len2 = len + 1;
                        sbuf[len] = '\n';
                        break;
                    case HistoryInfoHelper.MSG_CACHE_CLEAR_ALL /*114*/:
                        len2 = len + 1;
                        sbuf[len] = '\r';
                        break;
                    case 't':
                        len2 = len + 1;
                        sbuf[len] = '\t';
                        break;
                    case Service.UUCP_PATH /*117*/:
                        len2 = len + 1;
                        r6 = new char[4];
                        i++;
                        r6[0] = chars[i];
                        i++;
                        r6[1] = chars[i];
                        i++;
                        r6[2] = chars[i];
                        i++;
                        r6[3] = chars[i];
                        sbuf[len] = (char) Integer.parseInt(new String(r6), 16);
                        break;
                    case ItemSize.ITEM_118 /*118*/:
                        len2 = len + 1;
                        sbuf[len] = '\u000b';
                        break;
                    case 'x':
                        len2 = len + 1;
                        i++;
                        i++;
                        sbuf[len] = (char) ((digits[chars[i]] * 16) + digits[chars[i]]);
                        break;
                    default:
                        throw new JSONException("unclosed.str.lit");
                }
            }
            i++;
            len = len2;
        }
        return new String(sbuf, 0, len);
    }

    public final boolean isBlankInput() {
        int i = 0;
        while (true) {
            char chLocal = charAt(i);
            if (chLocal == JSONLexer.EOI) {
                return true;
            }
            if (!isWhitespace(chLocal)) {
                return false;
            }
            i++;
        }
    }

    public final void skipWhitespace() {
        while (this.ch <= '/') {
            if (this.ch == ' ' || this.ch == '\r' || this.ch == '\n' || this.ch == '\t' || this.ch == '\f' || this.ch == '\b') {
                next();
            } else if (this.ch == '/') {
                skipComment();
            } else {
                return;
            }
        }
    }

    private void scanStringSingleQuote() {
        this.np = this.bp;
        this.hasSpecial = false;
        while (true) {
            char chLocal = next();
            if (chLocal == '\'') {
                this.token = 4;
                next();
                return;
            } else if (chLocal == JSONLexer.EOI) {
                if (isEOF()) {
                    throw new JSONException("unclosed single-quote string");
                }
                putChar(JSONLexer.EOI);
            } else if (chLocal == '\\') {
                if (!this.hasSpecial) {
                    this.hasSpecial = true;
                    if (this.sp > this.sbuf.length) {
                        char[] newsbuf = new char[(this.sp * 2)];
                        System.arraycopy(this.sbuf, 0, newsbuf, 0, this.sbuf.length);
                        this.sbuf = newsbuf;
                    }
                    copyTo(this.np + 1, this.sp, this.sbuf);
                }
                chLocal = next();
                switch (chLocal) {
                    case '\"':
                        putChar('\"');
                        break;
                    case '\'':
                        putChar('\'');
                        break;
                    case '/':
                        putChar('/');
                        break;
                    case '0':
                        putChar('\u0000');
                        break;
                    case '1':
                        putChar('\u0001');
                        break;
                    case '2':
                        putChar('\u0002');
                        break;
                    case '3':
                        putChar('\u0003');
                        break;
                    case '4':
                        putChar('\u0004');
                        break;
                    case '5':
                        putChar('\u0005');
                        break;
                    case '6':
                        putChar('\u0006');
                        break;
                    case '7':
                        putChar('\u0007');
                        break;
                    case UikitEventType.UIKIT_ADD_DETAIL_CARDS /*70*/:
                    case 'f':
                        putChar('\f');
                        break;
                    case '\\':
                        putChar('\\');
                        break;
                    case Service.TACNEWS /*98*/:
                        putChar('\b');
                        break;
                    case 'n':
                        putChar('\n');
                        break;
                    case HistoryInfoHelper.MSG_CACHE_CLEAR_ALL /*114*/:
                        putChar('\r');
                        break;
                    case 't':
                        putChar('\t');
                        break;
                    case Service.UUCP_PATH /*117*/:
                        putChar((char) Integer.parseInt(new String(new char[]{next(), next(), next(), next()}), 16));
                        break;
                    case ItemSize.ITEM_118 /*118*/:
                        putChar('\u000b');
                        break;
                    case 'x':
                        putChar((char) ((digits[next()] * 16) + digits[next()]));
                        break;
                    default:
                        this.ch = chLocal;
                        throw new JSONException("unclosed single-quote string");
                }
            } else if (!this.hasSpecial) {
                this.sp++;
            } else if (this.sp == this.sbuf.length) {
                putChar(chLocal);
            } else {
                char[] cArr = this.sbuf;
                int i = this.sp;
                this.sp = i + 1;
                cArr[i] = chLocal;
            }
        }
    }

    protected final void putChar(char ch) {
        if (this.sp == this.sbuf.length) {
            char[] newsbuf = new char[(this.sbuf.length * 2)];
            System.arraycopy(this.sbuf, 0, newsbuf, 0, this.sbuf.length);
            this.sbuf = newsbuf;
        }
        char[] cArr = this.sbuf;
        int i = this.sp;
        this.sp = i + 1;
        cArr[i] = ch;
    }

    public final void scanNumber() {
        this.np = this.bp;
        if (this.ch == '-') {
            this.sp++;
            next();
        }
        while (this.ch >= '0' && this.ch <= '9') {
            this.sp++;
            next();
        }
        boolean isDouble = false;
        if (this.ch == '.') {
            this.sp++;
            next();
            isDouble = true;
            while (this.ch >= '0' && this.ch <= '9') {
                this.sp++;
                next();
            }
        }
        if (this.ch == 'L') {
            this.sp++;
            next();
        } else if (this.ch == 'S') {
            this.sp++;
            next();
        } else if (this.ch == 'B') {
            this.sp++;
            next();
        } else if (this.ch == 'F') {
            this.sp++;
            next();
            isDouble = true;
        } else if (this.ch == 'D') {
            this.sp++;
            next();
            isDouble = true;
        } else if (this.ch == 'e' || this.ch == 'E') {
            this.sp++;
            next();
            if (this.ch == '+' || this.ch == '-') {
                this.sp++;
                next();
            }
            while (this.ch >= '0' && this.ch <= '9') {
                this.sp++;
                next();
            }
            if (this.ch == 'D' || this.ch == 'F') {
                this.sp++;
                next();
            }
            isDouble = true;
        }
        if (isDouble) {
            this.token = 3;
        } else {
            this.token = 2;
        }
    }

    public final long longValue() throws NumberFormatException {
        long limit;
        int i;
        long result = 0;
        boolean negative = false;
        if (this.np == -1) {
            this.np = 0;
        }
        int i2 = this.np;
        int max = this.np + this.sp;
        if (charAt(this.np) == '-') {
            negative = true;
            limit = Long.MIN_VALUE;
            i = i2 + 1;
        } else {
            limit = -9223372036854775807L;
            i = i2;
        }
        if (i < max) {
            result = (long) (-(charAt(i) - 48));
            i++;
        }
        while (i < max) {
            i2 = i + 1;
            char chLocal = charAt(i);
            if (chLocal == 'L' || chLocal == 'S' || chLocal == 'B') {
                break;
            }
            int digit = chLocal - 48;
            if (result < MULTMIN_RADIX_TEN) {
                throw new NumberFormatException(numberString());
            }
            result *= 10;
            if (result < ((long) digit) + limit) {
                throw new NumberFormatException(numberString());
            }
            result -= (long) digit;
            i = i2;
        }
        i2 = i;
        if (!negative) {
            return -result;
        }
        if (i2 > this.np + 1) {
            return result;
        }
        throw new NumberFormatException(numberString());
    }

    public final Number decimalValue(boolean decimal) {
        char chLocal = charAt((this.np + this.sp) - 1);
        if (chLocal == 'F') {
            try {
                return Float.valueOf(Float.parseFloat(numberString()));
            } catch (NumberFormatException ex) {
                throw new JSONException(ex.getMessage() + ", " + info());
            }
        } else if (chLocal == 'D') {
            return Double.valueOf(Double.parseDouble(numberString()));
        } else {
            if (decimal) {
                return decimalValue();
            }
            return Double.valueOf(doubleValue());
        }
    }

    public final BigDecimal decimalValue() {
        return new BigDecimal(numberString());
    }

    public static boolean isWhitespace(char ch) {
        return ch <= ' ' && (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t' || ch == '\f' || ch == '\b');
    }
}
