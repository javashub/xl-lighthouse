import moment from 'moment';
import 'moment-timezone';

const timezone = getSystemTimeZone();

export const DateTimeFormat = 'YYYY-MM-DD HH:mm:ss';

export const DateFormat = 'YYYY-MM-DD';

export const MonthFormat = 'YYYY-MM';

export const YearFormat = 'YYYY';

export function getSystemTimeZone() : string {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

export function formatTimeStamp(timestamp, format):string {
    return moment(timestamp).tz(timezone).format(format)
}

export function getDailyStartTimestamp():number{
    return moment().tz(timezone).startOf('day').valueOf();
}

export function getDailyEndTimestamp():number{
    return moment().tz(timezone).endOf('day').valueOf();
}

export function getDayStartTimestamp(timestamp):number{
    return moment(timestamp).tz(timezone).startOf('day').valueOf();
}

export function getDayEndTimestamp(timestamp):number{
    return moment(timestamp).tz(timezone).endOf('day').valueOf();
}

export function convertDateToTimestamp(dateString, dateFormat) {
    return moment.tz(dateString, dateFormat,timezone).valueOf();
}

export function getDayBefore(timestamp,beforeDays) {
    return moment(timestamp).tz(timezone).subtract(beforeDays, 'days');
}

export function getDayAfter(timestamp,afterDays) {
    return moment(timestamp).tz(timezone).add(afterDays, 'days');
}




